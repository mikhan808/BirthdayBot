[CmdletBinding()]
param(
    [Parameter(Mandatory = $true)]
    [ValidateScript({ Test-Path -LiteralPath $_ -PathType Leaf })]
    [string]$SourceGbak,

    [Parameter(Mandatory = $true)]
    [ValidateScript({ Test-Path -LiteralPath $_ -PathType Leaf })]
    [string]$TargetGbak,

    [Parameter(Mandatory = $true)]
    [string]$SourceDatabase,

    [Parameter(Mandatory = $true)]
    [string]$BackupFile,

    [Parameter(Mandatory = $true)]
    [string]$TargetDatabase,

    [string]$SourceUser = "SYSDBA",
    [string]$TargetUser = "SYSDBA",

    [switch]$WritesStopped
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

if (-not $WritesStopped) {
    throw "Stop BirthdayBot and all other writers, then run the script with -WritesStopped."
}

if (Test-Path -LiteralPath $BackupFile) {
    throw "Backup file already exists: $BackupFile. Choose a new file to avoid overwriting a recovery point."
}

$backupDirectory = Split-Path -Parent $BackupFile
if (-not $backupDirectory -or -not (Test-Path -LiteralPath $backupDirectory -PathType Container)) {
    throw "Backup directory does not exist: $backupDirectory"
}

function ConvertTo-PlainText {
    param(
        [Parameter(Mandatory = $true)]
        [Security.SecureString]$SecureValue
    )

    $pointer = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($SecureValue)
    try {
        return [Runtime.InteropServices.Marshal]::PtrToStringBSTR($pointer)
    }
    finally {
        [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($pointer)
    }
}

function Invoke-Gbak {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Executable,

        [Parameter(Mandatory = $true)]
        [string]$User,

        [Parameter(Mandatory = $true)]
        [string]$Password,

        [Parameter(Mandatory = $true)]
        [string[]]$GbakArguments
    )

    $previousUser = $env:ISC_USER
    $previousPassword = $env:ISC_PASSWORD

    try {
        $env:ISC_USER = $User
        $env:ISC_PASSWORD = $Password
        & $Executable @GbakArguments
        if ($LASTEXITCODE -ne 0) {
            throw "gbak failed with exit code $LASTEXITCODE"
        }
    }
    finally {
        $env:ISC_USER = $previousUser
        $env:ISC_PASSWORD = $previousPassword
    }
}

$sourcePassword = ConvertTo-PlainText (Read-Host "Password for $SourceUser on the source server" -AsSecureString)
$targetPassword = ConvertTo-PlainText (Read-Host "Password for $TargetUser on the Firebird 5 server" -AsSecureString)

try {
    Write-Host "Creating a logical backup with the source-server gbak..."
    $backupParameters = @{
        Executable    = $SourceGbak
        User          = $SourceUser
        Password      = $sourcePassword
        GbakArguments = @("-backup", "-verbose", $SourceDatabase, $BackupFile)
    }
    Invoke-Gbak @backupParameters

    Write-Host "Restoring to a new database with the Firebird 5 gbak..."
    $restoreParameters = @{
        Executable    = $TargetGbak
        User          = $TargetUser
        Password      = $targetPassword
        GbakArguments = @("-create", "-verbose", $BackupFile, $TargetDatabase)
    }
    Invoke-Gbak @restoreParameters

    Write-Host "Migration restore completed successfully. Validate the new database before switching BirthdayBot."
}
finally {
    $sourcePassword = $null
    $targetPassword = $null
}
