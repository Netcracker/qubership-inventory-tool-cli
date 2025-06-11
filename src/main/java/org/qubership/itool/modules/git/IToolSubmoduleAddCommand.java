/*
 * Copyright (C) 2011, GitHub Inc. and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0 which is available at
 * https://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */
package org.qubership.itool.modules.git;

import org.eclipse.jgit.api.AddCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.SubmoduleAddCommand;
import org.eclipse.jgit.api.TransportCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.JGitInternalException;
import org.eclipse.jgit.api.errors.NoFilepatternException;
import org.eclipse.jgit.errors.ConfigInvalidException;
import org.eclipse.jgit.internal.JGitText;
import org.eclipse.jgit.internal.submodule.SubmoduleValidator;
import org.eclipse.jgit.lib.*;
import org.eclipse.jgit.storage.file.FileBasedConfig;
import org.eclipse.jgit.submodule.SubmoduleWalk;
import org.eclipse.jgit.treewalk.filter.PathFilter;
import org.eclipse.jgit.treewalk.filter.TreeFilter;

import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;

/**
 * A class used to execute a submodule add command.
 *
 * This will clone the configured submodule, register the submodule in the
 * .gitmodules file and the repository config file, and also add the submodule
 * and .gitmodules file to the index.
 *
 * Copy-pasted from original SubmoduleAddCommand to be able to reuse IToolCloneCommand
 * to solve the issue in the for corner case when submodule is already exists in another branch,
 * but absent in current one.
 *
 * @see <a href=
 *      "http://www.kernel.org/pub/software/scm/git/docs/git-submodule.html"
 *      >Git documentation about submodules</a>
 */
public class IToolSubmoduleAddCommand extends SubmoduleAddCommand {
	private String name;

	private String path;

	private String uri;

	private ProgressMonitor monitor;

    /**
	 * Constructor for SubmoduleAddCommand.
	 *
	 * @param repo a {@link Repository} object.
	 */
	public IToolSubmoduleAddCommand(Repository repo) {
		super(repo);
	}

	/**
	 * Set the submodule name
	 *
	 * @param name name
	 * @return this command
	 * @since 5.1
	 */
	public IToolSubmoduleAddCommand setName(String name) {
		this.name = name;
		return this;
	}

	/**
	 * Set repository-relative path of submodule
	 *
	 * @param path (with <code>/</code> as separator)
	 * @return this command
	 */
	public IToolSubmoduleAddCommand setPath(String path) {
		this.path = path;
		return this;
	}

	/**
	 * Set URI to clone submodule from
	 *
	 * @param uri a {@link String} object.
	 * @return this command
	 */
	public IToolSubmoduleAddCommand setURI(String uri) {
		this.uri = uri;
		return this;
	}

	/**
	 * The progress monitor associated with the clone operation. By default,
	 * this is set to <code>NullProgressMonitor</code>
	 *
	 * @see NullProgressMonitor
	 * @param monitor
	 *            a {@link ProgressMonitor} object.
	 * @return this command
	 */
	public IToolSubmoduleAddCommand setProgressMonitor(ProgressMonitor monitor) {
		this.monitor = monitor;
		return this;
	}

    /**
     * Is the configured already a submodule in the index?
     *
     * @return true if submodule exists in index, false otherwise
     * @throws java.io.IOException
     *             if an IO error occurred
     */
    @Override
    protected boolean submoduleExists() throws IOException {
        TreeFilter filter = PathFilter.create(path);
        try (SubmoduleWalk w = SubmoduleWalk.forIndex(repo)) {
            return w.setFilter(filter).next();
        }
    }

	/**
	 * {@inheritDoc}
	 * <p>
	 * Executes the {@code SubmoduleAddCommand}
	 *
	 * The {@code Repository} instance returned by this command needs to be
	 * closed by the caller to free resources held by the {@code Repository}
	 * instance. It is recommended to call this method as soon as you don't need
	 * a reference to this {@code Repository} instance anymore.
	 */
	@Override
	public Repository call() throws GitAPIException {
		checkCallable();
		if (path == null || path.length() == 0)
			throw new IllegalArgumentException(JGitText.get().pathNotConfigured);
		if (uri == null || uri.length() == 0)
			throw new IllegalArgumentException(JGitText.get().uriNotConfigured);
		if (name == null || name.length() == 0) {
			// Use the path as the default.
			name = path;
		}

		try {
			SubmoduleValidator.assertValidSubmoduleName(name);
			SubmoduleValidator.assertValidSubmodulePath(path);
			SubmoduleValidator.assertValidSubmoduleUri(uri);
		} catch (SubmoduleValidator.SubmoduleValidationException e) {
			throw new IllegalArgumentException(e.getMessage());
		}

		try {
			if (submoduleExists())
				throw new JGitInternalException(MessageFormat.format(
						JGitText.get().submoduleExists, path));
		} catch (IOException e) {
			throw new JGitInternalException(e.getMessage(), e);
		}

		final String resolvedUri;
		try {
			resolvedUri = SubmoduleWalk.getSubmoduleRemoteUrl(repo, uri);
		} catch (IOException e) {
			throw new JGitInternalException(e.getMessage(), e);
		}
		// Clone submodule repository
		File moduleDirectory = SubmoduleWalk.getSubmoduleDirectory(repo, path);
		IToolCloneCommand clone = new IToolCloneCommand();
		configure(clone);
		clone.setForce(true);
		clone.setDirectory(moduleDirectory);
		clone.setGitDir(new File(new File(repo.getCommonDirectory(),
				Constants.MODULES), path));
		clone.setURI(resolvedUri);
		if (monitor != null)
			clone.setProgressMonitor(monitor);
		Repository subRepo = null;
		try (Git git = clone.call()) {
			subRepo = git.getRepository();
			subRepo.incrementOpen();
		}

		// Save submodule URL to parent repository's config
		StoredConfig config = repo.getConfig();
		config.setString(ConfigConstants.CONFIG_SUBMODULE_SECTION, name,
				ConfigConstants.CONFIG_KEY_URL, resolvedUri);
		try {
			config.save();
		} catch (IOException e) {
			throw new JGitInternalException(e.getMessage(), e);
		}

		// Save path and URL to parent repository's .gitmodules file
		FileBasedConfig modulesConfig = new FileBasedConfig(new File(
				repo.getWorkTree(), Constants.DOT_GIT_MODULES), repo.getFS());
		try {
			modulesConfig.load();
			modulesConfig.setString(ConfigConstants.CONFIG_SUBMODULE_SECTION,
					name, ConfigConstants.CONFIG_KEY_PATH, path);
			modulesConfig.setString(ConfigConstants.CONFIG_SUBMODULE_SECTION,
					name, ConfigConstants.CONFIG_KEY_URL, uri);
			modulesConfig.save();
		} catch (IOException | ConfigInvalidException e) {
			throw new JGitInternalException(e.getMessage(), e);
		}

		AddCommand add = new AddCommand(repo);
		// Add .gitmodules file to parent repository's index
		add.addFilepattern(Constants.DOT_GIT_MODULES);
		// Add submodule directory to parent repository's index
		add.addFilepattern(path);
		try {
			add.call();
		} catch (NoFilepatternException e) {
			throw new JGitInternalException(e.getMessage(), e);
		}

		return subRepo;
	}
}
