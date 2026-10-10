$ErrorActionPreference = "Continue"
$sdk = "C:\Users\Service-Riyalo\AppData\Local\Android\Sdk"
$bt = "$sdk\build-tools\36.0.0"
$platform = "$sdk\platforms\android-34\android.jar"
$java = "C:\Program Files\Eclipse Adoptium\jdk-25.0.3.9-hotspot\bin"

$root = "C:\Users\Service-Riyalo\downloads\quotation-generator\Quotation-Generator\android\app"
$offline = "C:\Users\Service-Riyalo\downloads\quotation-generator\Quotation-Generator\android\offline"
$build = "$root\build"
$out = "C:\Users\Service-Riyalo\downloads\quotation-generator\Quotation-Generator\release\Quotation-Generator.apk"

# clean
if (Test-Path $build) { Remove-Item -Recurse -Force $build }
New-Item -ItemType Directory -Force -Path "$build\compiled" | Out-Null
New-Item -ItemType Directory -Force -Path "$build\gen" | Out-Null
New-Item -ItemType Directory -Force -Path "$build\classes" | Out-Null
New-Item -ItemType Directory -Force -Path "$build\dex" | Out-Null

Write-Host "[1/7] aapt2 compile resources"
& "$bt\aapt2.exe" compile --dir "$root\res" -o "$build\compiled\res.zip"
if ($LASTEXITCODE -ne 0) { throw "aapt2 compile failed" }

Write-Host "[2/7] aapt2 link (resources + manifest + www assets -> R.java + base apk)"
& "$bt\aapt2.exe" link -o "$build\base.apk" -I "$platform" `
    --manifest "$root\AndroidManifest.xml" `
    --java "$build\gen" `
    -A "$offline" `
    --min-sdk-version 21 --target-sdk-version 34 `
    "$build\compiled\res.zip"
if ($LASTEXITCODE -ne 0) { throw "aapt2 link failed" }

Write-Host "[3/7] javac (against android.jar + generated R.java)"
$rjava = Get-ChildItem "$build\gen" -Recurse -Filter "*.java" | Select-Object -ExpandProperty FullName
& "$java\javac.exe" -source 1.8 -target 1.8 -bootclasspath "$platform" `
    -d "$build\classes" `
    "$root\src\com\riyalo\quotation\app\MainActivity.java" $rjava 2>&1 | ForEach-Object { Write-Host $_ }
if ($LASTEXITCODE -ne 0) { throw "javac failed" }

Write-Host "[4/7] d8 (class -> dex)"
& "$bt\d8.bat" --release --lib "$platform" --min-api 21 `
    --output "$build\dex" `
    (Get-ChildItem "$build\classes" -Recurse -Filter "*.class" | Select-Object -ExpandProperty FullName)
if ($LASTEXITCODE -ne 0) { throw "d8 failed" }

Write-Host "[5/7] add classes.dex into apk"
Set-Location "$build"
& "$java\jar.exe" uf "$build\base.apk" -C "$build\dex" classes.dex
if ($LASTEXITCODE -ne 0) { throw "jar update failed" }

Write-Host "[6/7] zipalign"
& "$bt\zipalign.exe" -f 4 "$build\base.apk" "$build\aligned.apk"
if ($LASTEXITCODE -ne 0) { throw "zipalign failed" }

Write-Host "[7/7] sign with debug keystore"
$ks = "$root\debug.keystore"
if (-not (Test-Path $ks)) {
    & "$java\keytool.exe" -genkeypair -v -keystore $ks -storepass android -alias androiddebugkey `
        -keypass android -keyalg RSA -keysize 2048 -validity 10000 `
        -dname "CN=QuotationGenerator-OEM, OU=Dev, O=Riyalo, L=Muscat, S=MA, C=OM"
    if ($LASTEXITCODE -ne 0) { throw "keytool failed" }
}
& "$bt\apksigner.bat" sign --ks $ks --ks-pass pass:android --key-pass pass:android `
    --out $out "$build\aligned.apk"
if ($LASTEXITCODE -ne 0) { throw "apksigner failed" }

Write-Host "DONE -> $out"
Get-Item $out | Select-Object FullName, Length