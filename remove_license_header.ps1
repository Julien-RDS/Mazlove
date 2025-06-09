$files = Get-ChildItem -Path "common\src\main\java" -Recurse -Include "*.java"

foreach ($file in $files) {
    $content = Get-Content $file.FullName
    
    # Remove license header
    $content = $content | Where-Object { 
        $_ -notmatch "Licensed to Codename One LTD under one" -and 
        $_ -notmatch "or more contributor license agreements" -and 
        $_ -notmatch "See the NOTICE file distributed with this work" -and 
        $_ -notmatch "regarding copyright ownership" -and 
        $_ -notmatch "Codename One licenses this file" -and 
        $_ -notmatch "to you under the Apache License" -and 
        $_ -notmatch "http://www.apache.org/licenses/LICENSE-2.0" -and 
        $_ -notmatch "Unless required by applicable law" -and 
        $_ -notmatch "software distributed under the License" -and 
        $_ -notmatch "`"AS IS`" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY" -and 
        $_ -notmatch "KIND, either express or implied" -and 
        $_ -notmatch "See the License for the specific language" -and 
        $_ -notmatch "governing permissions and limitations" -and 
        $_ -notmatch "under the License" -and 
        $_ -notmatch "^\s*$"  # Remove empty lines
    }

    # Save modified file
    $content | Set-Content $file.FullName
}
