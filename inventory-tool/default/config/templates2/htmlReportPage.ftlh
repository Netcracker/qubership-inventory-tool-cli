[#ftl strip_whitespace=false]
[#outputformat "RTF"][#t]
[#assign aDateTime = .now][#t]
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01//EN" "http://www.w3.org/TR/html4/strict.dtd">
<html>
<head>
    <meta http-equiv="content-type" content="text/html; charset=utf-8"/>

    <title>Report on [=aDateTime?iso("UTC")] UTC</title>

    <link rel="stylesheet" type="text/css"
          href="https://cdn.datatables.net/r/ju-1.11.4/jqc-1.11.3,dt-1.10.8/datatables.min.css"/>

    <link rel="stylesheet" type="text/css"
          href="./css/style.css"/>

    <script type="text/javascript"
            src="https://cdn.datatables.net/r/ju-1.11.4/jqc-1.11.3,dt-1.10.8/datatables.min.js"></script>

    <script type="text/javascript" charset="utf-8">
        $(document).ready(function() {
            $('#main-table').dataTable({
                pageLength: -1
            });
        } );

        $( function() {
            $( document ).tooltip();
          } );
    </script>
</head>
<body>
<div class="container">
    <p>Report is generated at [=aDateTime?iso("UTC")] UTC</p>

    <table id="main-table" style="width:100%">
        <thead>
        <tr>
            <th>#</th>
            <th>Name</th>
            <th>Owner</th>
            <th>Description</th>
            <th>Tech stack</th>
            <th>License</th>
        </tr>
        </thead>
        <tbody>
        [#list components![]][#t]
        [#items as entry][#t]
            <tr>
                <td>[=entry?counter]</td>
                <td><a href="[=(entry.C.repository)!""]">[=(entry.C.id)!"❌&nbsp;"]</a></td>
                <td>[=(entry.C.details.owner)!"❌&nbsp;"]</td>
                <td>[=(entry.C.details.description)!"❌&nbsp;"]</td>
                <td>[#list (entry.TECH)!["no"] as tech][=tech][#sep], [/#sep][#else ]❌&nbsp;[/#list]</td>
                <td>[=(entry.C.validationResults.license.result)!"❌&nbsp;"]</td>
            </tr>
        [/#items][#t]
        [#else ][#t]
        No data provided.
        [/#list][#t]
        </tbody>
    </table>
</div>
</body>
</html>
[/#outputformat]