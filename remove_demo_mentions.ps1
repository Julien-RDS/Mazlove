$files = Get-ChildItem -Path "common\src\main\java" -Recurse -Include "*.java"

foreach ($file in $files) {
    $content = Get-Content $file.FullName
    
    # Remove demo/test mentions
    $content = $content | Where-Object { 
        $_ -notmatch "createDemoMainWindowEntity" -and 
        $_ -notmatch "createProviderDemoModel" -and 
        $_ -notmatch "createDemoAddOns" -and 
        $_ -notmatch "demo" -and 
        $_ -notmatch "test" -and 
        $_ -notmatch "example" -and 
        $_ -notmatch "sample" -and 
        $_ -notmatch "trial" -and 
        $_ -notmatch "template" -and 
        $_ -notmatch "^\s*$"  # Remove empty lines
    }

    # Save modified file
    $content | Set-Content $file.FullName
}
