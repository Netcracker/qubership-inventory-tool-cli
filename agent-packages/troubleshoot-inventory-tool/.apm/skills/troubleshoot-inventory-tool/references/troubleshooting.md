# Qubership Inventory Tool CLI — troubleshooting

The Qubership Inventory Tool CLI builds a graph data model from component repositories. It runs as a fat jar or as
the `itool` Docker image. This catalog contains only failures whose symptoms, causes, checks, and fixes are traceable
to this repository.

## Startup and configuration

### `GIT facilities will not be available` although offline mode is off

**Symptoms:**

* The log contains `No login or password provided, GIT facilities will not be available`.
* It may also contain `No login or password provided, Confluence facilities will not be available`.
* No `Offline mode` line is present.
* Repository tasks run but clone nothing.

**Root cause:**

`GitAdapterBuilder.create` returns `null` when either `login` or `password` is absent from the resolved config, even
with `offlineMode` off (`src/main/java/org/qubership/itool/modules/git/GitAdapterBuilder.java:43-47`). `login` comes
from the `-l` / `--login` option or the `login` property; `password` is filled by `ConfigProvider.fillPassword`.

**How to check:**

1. Confirm both halves are reported missing together:

   ```bash
   grep -n "No login or password provided" <log-file>
   ```

2. Check whether `-l` / `--login` was passed, or `login` is set in the profile:

   ```bash
   grep -n "^ *login" inventory-tool/default/config/profiles/*.properties
   ```

3. Check for a password-source warning. Its presence means the configured file was not read:

   ```bash
   grep -n "Exception when reading the password from a file" <log-file>
   ```

**How to fix:**

1. Pass the login explicitly and let the password come from `passwordSource`:

   ```bash
   java -jar inventory-tool.jar exec -l <login>
   ```

2. Or set `login` in the profile the run selects, alongside `passwordSource`.

**Sources:**

* `src/main/java/org/qubership/itool/modules/git/GitAdapterBuilder.java:43-47`
* `src/main/java/org/qubership/itool/modules/confluence/ConfluenceClientBuilder.java:48-52`
* `src/main/java/org/qubership/itool/cli/config/ConfigProvider.java:79-104`
* `src/main/java/org/qubership/itool/cli/ExecCommand.java:51-54`

### The command exits with code 4 and `Configuration loading failed`

**Symptoms:**

* The process exits with code `4` and the log contains `Configuration loading failed: <message>` with a stack trace.
* No task ever starts — there is no `========== Starting a flow` line.
* The stack trace identifies the configuration error that completed the loading future exceptionally.

**Root cause:**

Exit code `4` means the configuration-loading future completed with an `ExecutionException`
(`src/main/java/org/qubership/itool/cli/ConfigLoadingExecutionStrategy.java:91-99`). One required input is
`<configPath>/default/config/profiles/default.properties`: unlike the custom profile, this store is not optional
(`src/main/java/org/qubership/itool/cli/config/ConfigProvider.java:166-196`). The default `configPath` is the relative
path `inventory-tool` (`src/main/java/org/qubership/itool/cli/AbstractCommand.java:41-47`).

The neighboring exit codes come from the same block and narrow the cause: `2` Vertx not initialized, `3` configuration
loading timed out, `5` interrupted, `6` unexpected error during context initialization
(`ConfigLoadingExecutionStrategy.java:88-107`).

**How to check:**

1. Read the exit code recorded by the original invocation — the CI job's step result, or `$?` in the shell that ran
   it. Do not rerun the command to obtain it: if configuration then succeeds, the flow starts and begins cloning
   repositories and writing output. Exit `4` distinguishes this from an ordinary flow failure, which is exit `1`.

2. If the stack trace names the default profile, confirm it exists where the run looked for it:

   ```bash
   ls -l <configPath>/default/config/profiles/default.properties
   ```

3. If `--configPath` was not passed, confirm the working directory holds the `inventory-tool` tree, since the default
   is relative:

   ```bash
   ls -d inventory-tool/default/config/profiles
   ```

**How to fix:**

1. If the default profile is missing at the resolved path, run from the directory that contains the `inventory-tool`
   configuration tree, or point at it explicitly:

   ```bash
   java -jar inventory-tool.jar exec -l <login> -c /opt/inventory-tool
   ```

2. If the tree is missing entirely, restore the built `target/inventory-tool` tree or the image's
   `/app/inventory-tool` tree (`Dockerfile:10`).
3. If the stack trace identifies another configuration input, fix that named input instead; exit code `4` alone does
   not identify a missing default profile.

**How to avoid this issue:**

Pass `-c` with an absolute path in any automated invocation, so the run does not depend on the working directory.

**Data to collect:**

* The exit code and the full `Configuration loading failed` stack trace.
* The value of `-c` and the working directory the command ran from.

**Sources:**

* `src/main/java/org/qubership/itool/cli/ConfigLoadingExecutionStrategy.java:88-117`
* `src/main/java/org/qubership/itool/cli/config/ConfigProvider.java:166-205`
* `src/main/java/org/qubership/itool/cli/AbstractCommand.java:41-47`
* `Dockerfile:8-12`

### Settings from `--profile` have no effect

**Symptoms:**

* The run behaves exactly as the `default` profile prescribes, ignoring every value in the profile you named.
* No error or warning about the profile appears in the log.
* Typical follow-on symptom: the tool tries to reach `https://your.domain/inventory-tool-data.git`, the placeholder
  `git.superRepositoryUrl` from the shipped `default` profile.

**Root cause:**

The custom profile config store is registered with `.setOptional(true)`, so a path that does not exist is skipped
silently and only `default/config/profiles/default.properties` contributes
(`src/main/java/org/qubership/itool/cli/config/ConfigProvider.java:186-196`). A misspelled profile name, a missing
`.json` extension for a JSON profile, or a profile placed outside
`<configPath>/default/config/profiles/` all produce this. Profile names without a dot get `.properties` appended
(`ConfigProvider.java:178-185`).

**How to check:**

1. Read the exact name passed to `-p` / `--profile` from the command line or the CI job definition.
2. Confirm a file with that name exists where the store looks for it:

   ```bash
   ls -l inventory-tool/default/config/profiles/
   ```

3. If the run used a shipped placeholder such as `https://your.domain/inventory-tool-data.git`, confirm that the
   custom profile was intended to override that exact property.

**How to fix:**

1. Place the profile in `<configPath>/default/config/profiles/` and pass its name without the extension for a
   properties file, or with the extension for JSON:

   ```bash
   java -jar inventory-tool.jar exec -p custom            # custom.properties
   java -jar inventory-tool.jar exec -p custom_example.json
   ```

2. Or override the individual values on the command line, which bypasses profile resolution entirely. Write nested
   settings as JSON pointers, not in the dotted form the properties files use: `--set` treats a key beginning with
   `/` as a pointer and anything else as a flat key, while the code reads these values by pointer
   (`src/main/java/org/qubership/itool/cli/config/ConfigProvider.java:153-164`,
   `src/main/java/org/qubership/itool/utils/ConfigUtils.java:47-53`):

   ```bash
   java -jar inventory-tool.jar exec --set /git/superRepositoryUrl=<url> --set /release=<release>
   ```

**How to avoid this issue:**

Keep profiles under version control in `inventory-tool/default/config/profiles/` and reference them by the exact file
name.

**Sources:**

* `src/main/java/org/qubership/itool/cli/config/ConfigProvider.java:166-206`
* `src/main/java/org/qubership/itool/utils/ConfigUtils.java:47-53`
* `inventory-tool/default/config/profiles/default.properties:19-40`

### Configuration files come from `default` instead of the release you selected

**Symptoms:**

* The run uses domain lists or templates that belong to a different release than the `release` value you set.
* No warning names the fallback.

**Root cause:**

`ConfigUtils.getConfigFilePath` first builds `<configPath>/releases/<release>/<path>`; when that file does not exist it
silently retries under `<configPath>/default/<path>`
(`src/main/java/org/qubership/itool/utils/ConfigUtils.java:75-85`). A release directory that exists but is missing one
file therefore mixes release-specific and default configuration file by file.

**How to check:**

1. Read the `release` value the run resolved, from the profile or the `-r` / `--release` option.
2. List what that release directory actually contains:

   ```bash
   ls -R inventory-tool/releases/<release>/
   ```

3. Compare against the default set:

   ```bash
   ls -R inventory-tool/default/config/
   ```

**How to fix:**

1. Add the missing file to `inventory-tool/releases/<release>/config/` so the release copy wins.
2. Or correct the `release` value if the run selected the wrong one:

   ```bash
   java -jar inventory-tool.jar exec -r <release>
   ```

**Sources:**

* `src/main/java/org/qubership/itool/utils/ConfigUtils.java:75-85`

## Git and the super repository

### `Unable to open super repository`

**Symptoms:**

* The run fails with `Unable to open super repository <dir>/.git`.
* The flow ends with `Flow execution failed`.
* The directory exists and contains a `.git` entry.

**Root cause:**

A `.git` entry is present, so `prepareSuperRepoHandler` calls `Git.open` instead of cloning. `Git.open` throws an
`IOException`, which the tool reports as `Unable to open super repository`
(`src/main/java/org/qubership/itool/modules/git/GitAdapterImpl.java:479-489`).

**How to check:**

1. Read the directory from the message and confirm git agrees it is broken:

   ```bash
   git -C <git.superRepositoryDir> status
   ```

2. Check whether the core files exist:

   ```bash
   ls -l <git.superRepositoryDir>/.git/HEAD <git.superRepositoryDir>/.git/config
   ```

**How to fix:**

1. Choose a new backup path that does not exist, then move the repository there. This preserves the original files
   while allowing the next run to clone into the configured path:

   ```bash
   test ! -e <backup-dir> && mv <git.superRepositoryDir> <backup-dir>
   ```

2. Rerun the flow and confirm the log shows `Cloning the repository <url> to <dir>` rather than
   `Repository already exist`.

**Sources:**

* `src/main/java/org/qubership/itool/modules/git/GitAdapterImpl.java:479-489`

## Flow execution and progress

### The flow reports success but the result is incomplete

**Symptoms:**

* The log ends with `Flow succeeded. Undeploying and terminating.` and the process exits `0`.
* Earlier in the log there are one or more lines beginning
  `Failed to execute the task '<taskName>' [fiid=<id>], exception:` followed by a stack trace.
* Data that a failed task should have produced is missing from the dump.

**Root cause:**

When a task's `taskStart` method throws synchronously, `FlowTask.startInFlow` catches the `Throwable`, records an
internal error, and completes that task's promise (`src/main/java/org/qubership/itool/tasks/FlowTask.java:141-155`).
The flow can therefore finish successfully after this exact `Failed to execute the task` error. Failures that a task
reports by failing its promise still fail the flow.

**How to check:**

1. Count the swallowed failures:

   ```bash
   grep -n "Failed to execute the task" <log-file>
   ```

2. Read the error report out of the dump — it is under `/report/records` for current dumps and `/report` for legacy
   ones (`src/main/java/org/qubership/itool/modules/graph/GraphDumpSupport.java:174-190`):

   ```bash
   python3 -c "import json,sys; \
   d=json.load(open(sys.argv[1])); \
   print(json.dumps(d.get('report',{}),indent=2)[:4000])" <dump.json>
   ```

3. Locate the record whose message starts with `Failed to execute the task` and use its task name and stack trace.

**How to fix:**

1. Fix the exception named in each `Failed to execute the task` record, then rerun the complete flow.
2. Gate the CI job on both the process exit code and the dump report. For this caught-exception path, exit code `0`
   does not mean every task completed its work.

**How to avoid this issue:**

Fail the pipeline when the dump report contains `Failed to execute the task` records.

**Sources:**

* `src/main/java/org/qubership/itool/tasks/FlowTask.java:119-156`
* `src/main/java/org/qubership/itool/cli/AbstractCommand.java:63-77`
* `src/main/java/org/qubership/itool/cli/AbstractCommand.java:130-142`

### `Step '<name>' not found`

**Symptoms:**

* The log contains `========== Flow execution [fiid=<id>] failed: Step '<name>' not found`.
* The process exits `1` without running any task.
* A valid `progress/task.<name>.json` exists, so progress restoration succeeded before the step lookup.

**Root cause:**

With `--startStep`, the tool first restores `progress/task.<step>.json`, then looks for that step in the selected flow
(`src/main/java/org/qubership/itool/cli/FlowMainVerticle.java:140-162`). The `Step '<name>' not found` message is
reachable only when restoration succeeded but the name is absent from the flow sequence
(`src/main/java/org/qubership/itool/cli/FlowMainVerticle.java:208-230`). Without a readable progress file, the tool
instead reports `Can't restore progress file for '<step>'`.

**How to check:**

1. Confirm that the progress file named by the error exists and is valid JSON:

   ```bash
   python3 -m json.tool progress/task.<name>.json > /dev/null
   ```

2. List the flow resources in the packaged jar, then print the one used by the failing command. The `exec` command
   uses `ExecFlow.txt`; CI flow resources are below `org/qubership/itool/cli/ci/`:

   ```bash
   unzip -Z1 inventory-tool.jar | grep 'Flow\.txt$'
   unzip -p inventory-tool.jar org/qubership/itool/cli/ExecFlow.txt
   ```

3. Compare the restored step name against the selected flow.

**How to fix:**

1. Select a step that exists in the flow and has a matching saved progress file, for example:

   ```bash
   java -jar inventory-tool.jar exec -l <login> -ss parseInventoryFile
   ```

**Sources:**

* `src/main/java/org/qubership/itool/cli/FlowMainVerticle.java:208-230`
* `src/main/java/org/qubership/itool/cli/FlowMainVerticle.java:140-162`
* `src/main/resources/org/qubership/itool/cli/ExecFlow.txt:5-62`

### `Can't restore progress file for '<step>'`

**Symptoms:**

* The log contains `Can't restore progress file for '<step>'`, often with a `FileNotFoundException`.
* The flow fails immediately with `========== Flow execution [fiid=<id>] failed`.

**Root cause:**

`--startStep` restores the graph from `progress/task.<step>.json` relative to the working directory
(`src/main/java/org/qubership/itool/cli/FlowMainVerticle.java:140-158`). A prior task writes that file only when
`saveProgress` selects it (`src/main/java/org/qubership/itool/tasks/FlowTask.java:193-207`). Commands derived from
`AbstractCiCommand` set `saveProgress` to `false` (`src/main/java/org/qubership/itool/cli/ci/AbstractCiCommand.java:40-46`).

**How to check:**

1. Confirm whether the file exists, in the directory you launched from:

   ```bash
   ls -l progress/task.<step>.json
   ```

2. List which steps do have progress files:

   ```bash
   ls -1 progress/
   ```

3. Confirm `saveProgress` was on for the earlier run:

   ```bash
   grep -n "saveProgress" inventory-tool/default/config/profiles/*.properties
   ```

**How to fix:**

1. Run once from the beginning with progress saving on, then restart from a step:

   ```bash
   java -jar inventory-tool.jar exec -l <login> -sp true
   java -jar inventory-tool.jar exec -l <login> -sp true -ss <step>
   ```

2. Launch from the same working directory both times — the progress path is relative.

**Sources:**

* `src/main/java/org/qubership/itool/cli/FlowMainVerticle.java:140-162`
* `src/main/java/org/qubership/itool/tasks/FlowTask.java:193-207`
* `src/main/java/org/qubership/itool/cli/ci/AbstractCiCommand.java:40-46`

## CI mode and the Docker image

### `Either --docker=true or --inputDirectory must be specified! EXITTING!`

**Symptoms:**

* A CI command exits immediately with code `1` and logs
  `Either --docker=true or --inputDirectory must be specified! EXITTING!` — the misspelling is in the product.
* Nothing else runs; there is no flow banner.

**Root cause:**

`AbstractCiCommand` derives the input directory from `--dockerMode`: with `--docker=true` it defaults to
`/var/input`, and without it there is no default at all, so an explicit `--inputDirectory` is required and the command
calls `System.exit(1)` when neither is present
(`src/main/java/org/qubership/itool/cli/ci/AbstractCiCommand.java:88-95`). The image's standard entry points avoid it:
its `CMD` and its three helper scripts all pass `--dockerMode=true` (`Dockerfile:25`, `docker/ci-exec.sh:1-8`). This
error means the invocation that reached `AbstractCiCommand` had neither an explicit input directory nor Docker mode
enabled.

**How to check:**

1. Read the command line the job ran, and look for either flag:

   ```bash
   grep -n "ci-exec\|ci-assembly\|ci-obfuscate" <job-definition>
   ```

2. Confirm whether the run went through the image's entry point, which supplies `--dockerMode=true`, or invoked the
   jar directly.

**How to fix:**

1. Running the jar directly, name the input directory:

   ```bash
   java -jar inventory-tool.jar ci-exec --inputDirectory=<dir> --repository=<url> --componentName=<name>
   ```

2. Running the container, use the packaged entry point rather than overriding it, so `--dockerMode=true` is passed:

   ```bash
   docker run -v <in>:/var/input -v <out>:/var/output itool ci-exec --repository=<url> --componentName=<name>
   ```

**Sources:**

* `src/main/java/org/qubership/itool/cli/ci/AbstractCiCommand.java:80-95`
* `Dockerfile:14-25`
* `docker/ci-exec.sh:1-8`

### No result file appears in the output directory

**Symptoms:**

* The container exits `0` and the log ends with `Flow succeeded. Undeploying and terminating.`
* The log contains `Exception when saving progress file /var/output/result.<name>.json` with an
  `AccessDeniedException` or `Permission denied`.
* The mounted output directory is empty on the host.

**Root cause:**

`FlowContextImpl.dumpDataToFile` catches an `IOException` from writing the dump, logs it, and returns normally
(`src/main/java/org/qubership/itool/context/FlowContextImpl.java:186-199`). The image runs as UID 1001
(`Dockerfile:19-23`); whether a bind mount is writable depends on its mode, ownership, and access controls.

**How to check:**

1. Confirm the write failed rather than the dump being written elsewhere:

   ```bash
   grep -n "Exception when saving progress file" <container-log>
   ```

2. Read the mode and ownership of the host directory bound to `/var/output`:

   ```bash
   ls -ld <host-output-dir>
   ```

3. Confirm the container identity and test the mounted directory with that identity:

   ```bash
   docker run --rm --entrypoint id itool
   docker run --rm -v <host-output-dir>:/var/output --entrypoint test itool -w /var/output
   ```

**How to fix:**

1. Run the container as the user that owns the bind mount:

   ```bash
   docker run --user "$(id -u):$(id -g)" -v <host-output-dir>:/var/output itool ci-exec ...
   ```

2. Or create a new, dedicated output directory for the image's UID. The guard refuses to alter an existing path:

   ```bash
   test ! -e <new-output-dir> && install -d -m 0750 -o 1001 -g 1001 <new-output-dir>
   ```

**How to avoid this issue:**

Assert that the expected result file exists after the container exits, since the exit code will not report this
failure.

**Sources:**

* `src/main/java/org/qubership/itool/context/FlowContextImpl.java:186-199`
* `Dockerfile:19-25`
* `docs/docker/docker.md:15-40`

### Two components overwrite each other's output file

**Symptoms:**

* Fewer files appear in the output directory than components were processed.
* Multiple runs wrote to the same output directory with the same generated file name.

**Root cause:**

When `--outputFile` is not given, `SaveSingleResultVerticle` generates the name from the `--dumpResultsBy` strategy,
defaulting to `hash`: `result.<md5 of the repository URL>.json`
(`src/main/java/org/qubership/itool/tasks/ci/SaveSingleResultVerticle.java:55-75`). Two runs that pass the same
`--repository` — a multi-component repository processed twice, or the same repository built on two branches into one
output directory — therefore write the same file name and the second overwrites the first. The other two strategies
do not resolve every collision: `id` names the file after the component id, and `repo` after the repository's last
path segment (`src/main/java/org/qubership/itool/cli/ci/CiExecCommand.java:83-84`), so two builds of the *same*
component — two branches, two versions — still collide under all three.

**How to check:**

1. List what actually landed in the output directory and compare against the number of components run:

   ```bash
   ls -l <host-output-dir>
   ```

2. Confirm which `--repository` values the runs passed — identical values produce identical hashes.
3. Read the strategy in use from the job definition; the default is `hash` (`docs/docker/docker.md`).

**How to fix:**

1. Give each run an explicit, distinct output file. Use a filesystem-safe build identifier containing only letters,
   digits, dots, underscores, or hyphens; do not place a raw branch name such as `feature/foo` in the file name:

   ```bash
   docker run ... itool ci-exec --repository=<url> --componentName=<name> \
     --outputFile=result.<name>.<safe-build-id>.json
   ```

2. Or, when the collision is between *different* components that share one repository URL, switch the strategy so the
   name derives from the component instead:

   ```bash
   docker run ... itool ci-exec --repository=<url> --componentName=<name> --dumpResultsBy=id
   ```

3. Or give each build its own output directory, so no two runs can collide regardless of strategy.

**Sources:**

* `src/main/java/org/qubership/itool/tasks/ci/SaveSingleResultVerticle.java:49-100`
* `src/main/java/org/qubership/itool/cli/ci/CiExecCommand.java:83-84`
* `docs/docker/docker.md:15-40`

### `ci-exec` leaves the component in the `orphans` domain with defaulted identity

**Symptoms:**

* The error report contains a `CONF_ERROR` record reading `Reference was not found. Reference: domain` for the
  component.
* In the resulting dump the component hangs off a domain vertex whose `id` is `orphans` and which carries
  `"mockFlag": true`.
* The component's `type` is `unknown`, its `abbreviation` equals its `id`, and `details.name` is the repository's last
  path segment rather than the human-readable name.
* No `Parsing <path>/inventory.json from <component-id>` line appears in the log for the component.

**Root cause:**

Before parsing, `InitializeMockDomainVerticle` creates the configured mock domain and a component with `type = unknown`
(`src/main/java/org/qubership/itool/tasks/ci/InitializeMockDomainVerticle.java:53-91`). Parsing `inventory.json` is
what supplies the component abbreviation, name, type, and target domain
(`src/main/java/org/qubership/itool/modules/parsing/InventoryJsonParser.java:61-89`). If no inventory file is
discovered, relocation leaves the component under the mock domain and reports the missing domain reference
(`src/main/java/org/qubership/itool/tasks/ci/RelocateComponentsVerticle.java:45-123`).

**How to check:**

1. Confirm the missing-domain report signature:

   ```bash
   grep -n "Reference was not found. Reference: domain" <container-log>
   ```

2. Confirm that an edge from `orphans` targets the affected component:

   ```bash
   python3 -c "import json,sys; \
   d=json.load(open(sys.argv[1])); \
   [print(e.get('source'),'->',e.get('target')) \
   for e in d.get('graph',{}).get('edgeList',[]) if e.get('source')=='orphans']" <dump.json>
   ```

3. Confirm that no inventory file was discovered for this component:

   ```bash
   grep -n "Parsing .*inventory.json from" <container-log>
   ```

4. Confirm the file is absent from the directory bound to `/var/input`:

   ```bash
   ls -l <host-input-dir>/inventory.json <host-input-dir>/inventory-components.json
   ```

5. Confirm the container sees the expected input mount:

   ```bash
   docker run --rm -v <host-input-dir>:/var/input --entrypoint ls itool -l /var/input
   ```

6. Read the component identity from the dump:

   ```bash
   python3 -c "import json,sys; \
   d=json.load(open(sys.argv[1])); \
   [print(v.get('id'),v.get('type'),v.get('abbreviation'),v.get('mockFlag')) \
   for v in d.get('graph',{}).get('vertexList',[])]" <dump.json>
   ```

**How to fix:**

1. Add `inventory.json` to the component repository with the documented `id`, `name`, `type`, and `domain` fields.
2. For a repository holding several components, add `inventory-components.json` at the root alongside the nested
   `inventory.json` files (`docs/readMe.md`).

**Sources:**

* `src/main/java/org/qubership/itool/tasks/ci/InitializeMockDomainVerticle.java:53-103`
* `src/main/java/org/qubership/itool/modules/parsing/InventoryJsonParser.java:61-89`
* `src/main/java/org/qubership/itool/tasks/ci/RelocateComponentsVerticle.java:45-123`
* `src/main/java/org/qubership/itool/modules/graph/GraphImpl.java:404-430`
* `docs/docker/docker.md:1-13`
* `docs/readMe.md:1-60`

## Graph dumps and obfuscation

### `Graph dump version is not supported` or `Graph model version <n> not supported`

**Symptoms:**

* The run fails with `Graph dump version is not supported: <n>`.
* Or with `Graph model version <n> not supported`.

**Root cause:**

A dump carries a container `modelVersion` and a graph `modelVersion`. `GraphImpl.restoreGraphData` rejects a graph
model version newer than the reader implements (`src/main/java/org/qubership/itool/modules/graph/GraphImpl.java:442-446`).
`isGraphDumpSupported` also requires the container version to be the current or legacy version
(`src/main/java/org/qubership/itool/modules/graph/GraphDumpSupport.java:146-155`).

**How to check:**

1. Read both versions out of the dump:

   ```bash
   python3 -c "import json,sys; \
   d=json.load(open(sys.argv[1])); \
   print('container',d.get('modelVersion'),'graph',d.get('graph',{}).get('modelVersion'))" <dump.json>
   ```

2. Read the version of the tool that is reading it:

   ```bash
   java -jar inventory-tool.jar --version
   ```

3. If present, read the producing tool version from `/graph/root/meta/aditVersion`.

**How to fix:**

1. If the graph model is newer than the reader supports, use the tool version recorded in
   `/graph/root/meta/aditVersion` or a newer compatible reader. Do not edit version fields in the dump.
2. If the source data is available, regenerate the dump with the same tool version that will consume it.

**Sources:**

* `src/main/java/org/qubership/itool/modules/graph/GraphImpl.java:442-446`
* `src/main/java/org/qubership/itool/modules/graph/GraphDumpSupport.java:146-155`
* `src/main/java/org/qubership/itool/modules/processor/GraphMetaInfoSupport.java:103-106`
* `src/main/java/org/qubership/itool/modules/processor/GraphMetaInfoSupport.java:161-190`

### `ci-obfuscate` fails with `Can't load source graph`

**Symptoms:**

* The run fails with `Can't load source graph: <path>`.
* The process exits `1`.

**Root cause:**

`ObfuscationMainVerticle.start` joins `--inputDirectory` and the required `--inputFile`, reads that path as JSON, and
logs `Can't load source graph` when the read fails or produces no dump
(`src/main/java/org/qubership/itool/cli/obfuscate/ObfuscationMainVerticle.java:45-68`).

**How to check:**

1. Read the path from the message and confirm it exists inside the container's view:

   ```bash
   docker run --rm -v <host-input-dir>:/var/input --entrypoint ls itool -l /var/input
   ```

2. Confirm the file is valid JSON and not empty:

   ```bash
   python3 -m json.tool <host-input-dir>/<inputFile> > /dev/null
   ```

**How to fix:**

1. Pass `--inputFile` as a bare file name and let `--inputDirectory` default to `/var/input`:

   ```bash
   docker run -v <in>:/var/input -v <out>:/var/output itool ci-obfuscate \
     --inputFile=<dump>.json --outputFile=<dump>.obfuscated.json
   ```

2. If the file is empty or invalid JSON, regenerate the source dump and validate it before rerunning obfuscation.

**Sources:**

* `src/main/java/org/qubership/itool/cli/obfuscate/ObfuscationMainVerticle.java:44-70`
* `src/main/java/org/qubership/itool/cli/obfuscate/ObfuscateCommand.java:66-69`
* `docs/docker/docker.md:66-77`

### `Invalid or unsupported report format, skipping`

**Symptoms:**

* The log contains `Invalid or unsupported report format, skipping`.
* The restored graph is complete but its error report is empty.

**Root cause:**

`GraphDumpSupport.restoreFromJson` accepts a report either as a bare array (legacy dumps) or as an object at the
current container model version; anything else is replaced with an empty report and the restore continues
(`src/main/java/org/qubership/itool/modules/graph/GraphDumpSupport.java:98-113`). The graph itself is unaffected, so
this costs you the diagnostic record rather than the data.

**How to check:**

1. Read the shape of the report in the dump:

   ```bash
   python3 -c "import json,sys; \
   d=json.load(open(sys.argv[1])); r=d.get('report'); \
   print(type(r).__name__,d.get('modelVersion'))" <dump.json>
   ```

2. Compare the container `modelVersion` with the versions supported by the reader.

**How to fix:**

1. Regenerate the dump with the tool version that will read it, so the report is written in a shape that version
   accepts.
2. If the original run's errors matter and the dump cannot be regenerated, read them from that run's log instead —
   every report record is also logged as `<type> in <component>: <message>`
   (`src/main/java/org/qubership/itool/modules/report/GraphReportImpl.java:124`).

**Sources:**

* `src/main/java/org/qubership/itool/modules/graph/GraphDumpSupport.java:98-113`
* `src/main/java/org/qubership/itool/modules/graph/GraphDumpSupport.java:174-190`
* `src/main/java/org/qubership/itool/modules/report/GraphReportImpl.java:110-125`

## Confluence export

### Confluence pages are generated but never uploaded

**Symptoms:**

* The log contains one of `Offline mode, Confluence facilities will not be available`,
  `No URL to confluence provided, Confluence facilities will not be available`, or
  `No login or password provided, Confluence facilities will not be available`.
* Or the log contains `Parameter /confluence/uploadConfluencePages is set to 'none', pages upload skipped`.
* Page-generation tasks run and report success; nothing appears in the Confluence space.

**Root cause:**

`ConfluenceClientBuilder.create` returns `null` on any of three conditions — offline mode, no `confluenceUrl`, or a
missing login or password — and each is logged at `WARN` rather than failing the run
(`src/main/java/org/qubership/itool/modules/confluence/ConfluenceClientBuilder.java:36-53`). Separately, the shipped
`default` profile sets `confluence.uploadConfluencePages = none`, so upload is off unless a profile or the
`-u` option turns it on (`inventory-tool/default/config/profiles/default.properties:25-30`).

**How to check:**

1. Identify whether client creation or the upload selection disabled the upload:

   ```bash
   grep -n "Confluence facilities will not be available\|pages upload skipped" <log-file>
   ```

2. Read the upload selection from the selected profile or command line:

   ```bash
   grep -rn "uploadConfluencePages\|confluence.space" inventory-tool/default/config/profiles/
   ```

3. Confirm `confluenceUrl`, `login`, and `passwordSource` are present in the selected configuration.

**How to fix:**

1. Set `confluenceUrl` and the space in the profile, and supply the login and password the same way Git uses them.
2. Select which pages to upload, since `none` is the shipped default:

   ```bash
   java -jar inventory-tool.jar exec -l <login> -u all
   ```

3. Confirm `offlineMode` is `false` for this run.

**Sources:**

* `src/main/java/org/qubership/itool/modules/confluence/ConfluenceClientBuilder.java:36-53`
* `src/main/java/org/qubership/itool/tasks/confluence/ConfluenceUploadPagesVerticle.java:123-137`
* `src/main/java/org/qubership/itool/tasks/confluence/ConfluenceUploadPagesVerticle.java:369-379`
* `inventory-tool/default/config/profiles/default.properties:25-30`

## Extensions and custom tasks

### `No implementations found for the following tasks of the flow`

**Symptoms:**

* The run fails immediately with
  `No implementations found for the following tasks of the flow: [<stepName>, ...]`.
* The log may also contain `Path <configPath>/default/tasks does not exist or is not directory`.
* No task executes.

**Root cause:**

Each step name in a flow file is resolved by capitalizing it and appending `Verticle` or `Task`, then searching the
task class loader for a non-abstract `FlowTask` with either name. If any step has no match, the entire flow is rejected
(`src/main/java/org/qubership/itool/cli/FlowMainVerticle.java:249-307`). Custom tasks are loaded from
`<configPath>/default/tasks`; when that directory is absent, the factory warns and uses only the parent class loader
(`src/main/java/org/qubership/itool/factories/JavaAppContextVerticleFactory.java:48-69`).

**How to check:**

1. Read the unmatched step names from the message, then confirm what the custom-task directory holds:

   ```bash
   ls -l <configPath>/default/tasks/
   ```

2. For a step named `<foo>`, the loader looks for `Foo` + `Verticle` or `Task`. Search recursively, since a class in a
   Java package sits under its package directories rather than at the top level:

   ```bash
   find <configPath>/default/tasks -type f -name '*.class'
   ```

3. Confirm the step name's spelling against the flow definition the failing command actually uses. Flows are classpath
   resources inside the jar, so read them from there rather than from a source tree that a packaged installation does
   not have. List them first, then print the one flow that matters — `exec` uses `ExecFlow.txt`, the CI commands use
   the files under `cli/ci/`:

   ```bash
   unzip -Z1 inventory-tool.jar | grep 'Flow\.txt$'
   unzip -p inventory-tool.jar org/qubership/itool/cli/ci/CiExecFlow.txt
   ```

   For a flow contributed by an extension, list the extension's own jar instead.

**How to fix:**

1. Correct the step name in the flow file to match the class, remembering that the class name is the capitalized step
   plus `Verticle` or `Task`.
2. For a custom task, place the compiled `.class` file under `<configPath>/default/tasks/` — the directory takes plain
   class files, not a jar (`inventory-tool/default/tasks/readme.md`).
3. If the class is under a different configuration tree, pass that tree explicitly with `--configPath`.

**Sources:**

* `src/main/java/org/qubership/itool/cli/FlowMainVerticle.java:249-307`
* `src/main/java/org/qubership/itool/factories/JavaAppContextVerticleFactory.java:48-70`
* `inventory-tool/default/tasks/readme.md:1-4`
