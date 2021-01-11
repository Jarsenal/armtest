
#param($subscription,$resourceGroup,$storageAccountName,$container,$prefix,$start,$end,$target)
#vars
$subscription="SiMA PACE SFA-PROD"
$resourceGroup = "rg-sima-bib-prod"
$storageAccountName = "salrsseasimabibprod0001"
$container = "transactions"
$prefix = "transformed/" + "sap-settled-invoice"
$start = '03-01-2020 00:00:00'
$end = '03-14-2020 23:59:59'
$start = $start | Get-Date
$end = $end | Get-Date
$target = "C:/output"

write-host "Sub: $($subscription)"
write-host "RG: $($resourceGroup)"
write-host "Stroage: $($storageAccountName)"
write-host "Container: $($container)"
write-host "Prefix: $($prefix)"
write-host "Start: $($start)"
write-host "End: $($end)"



$MaxThreads = 10
$RunspacePool = [runspacefactory]::CreateRunspacePool(1, $MaxThreads)
$RunspacePool.Open()
[System.Collections.ArrayList]$Jobs = @()
[System.Collections.ArrayList]$powers = @()

$ScriptBlock = {
    param( $ctx, $container, $nameList, $jId, $start, $end,$target)
    #$myinput = (@{ctx=$ctx;container=$container;input=$input;jid=$jid;start=$start;end=$end}) | ConvertTo-Json

    #write-host "Started job $($JobId)"
    mkdir "$($target)/jobresults"
    #New-Item -Name "./jobresults/$($jId).orders.txt" -ItemType File
    #New-Item -Name "./jobresults/$($jId).transactions.txt" -ItemType File

    $count = 0
    
     ForEach($item in $nameList){
        Get-AzStorageBlobContent -Context $ctx -Container $container -Blob $item -Destination "$($target)" -force 
        $content = Get-Content -path $item
        $id = ($content | ConvertFrom-Json).values[0].id
        add-content "$($target)/jobresults/orders.$($jId).txt" "$($id)"
        add-content "$($target)/jobresults/transactions.$($jId).txt" ($item.replace("transformed/sap-settled-invoice/",""))
        $count += 1
     }
     return "Pulled $($count) files for job Id $($jId)"
    #write-host "Ended job $($JobId)"
}


$maxreturn = 1000
$total = 0
$token = $Null

# connect to azure
# a window will pop up and have you log in
#Connect-AzAccount
write-host "switch to $($subscription)"
Set-AzContext $subscription

# create folder and go into it
#$folder = "./pulledtransactions"
#mkdir "./jobresults"
#mkdir $folder
#cd $folder
#write-host "CD done"

# get storage account
$storageAccount = Get-AzStorageAccount -ResourceGroupName $resourceGroup -Name $storageAccountName
$ctx = $storageAccount.Context
write-host "Got Context"

$jobId = 1

    

do {

    write-host "Making request"

    if($token -ne $Null) {
        $results = Get-AzStorageBlob -Container $container -Context $ctx -MaxCount $maxreturn -Prefix $prefix -ContinuationToken $Token
    }
    else {
        $results = Get-AzStorageBlob -Container $container -Context $ctx -MaxCount $maxreturn -Prefix $prefix
    }
     

    write-host "Total results returned: $($results.Count)"

    $total += $results.Count

     if($results.Length -le 0) {
        Break;
     }

    write-host "First datetime stamp: $($results[0].LastModified.LocalDateTime)"

    $nameList = @()
    foreach($result in $results){
        if($result.LastModified.LocalDateTime -gt $start -and  $result.LastModified.LocalDateTime -lt $end){
            $nameList += $result.Name
        }
    }

    write-host $nameList

    if($nameList.count -gt 0){    
        write-host "Creating new job for $($nameList.count) transactions"

        $p = @{ctx=$ctx;container=$container;nameList=$nameList;jId=$jobId;start=$start;end=$end;target=$target}
        #write-host ($p | ConvertTo-Json )
        $PowerShell = [powershell]::Create()
        $PowerShell.RunspacePool = $RunspacePool
        $PowerShell.AddScript($ScriptBlock)
        $PowerShell.AddParameters($p)
        $Jobs += $PowerShell.BeginInvoke()
        $Powers += $PowerShell
        
        $jobId+=1
        
        write-host "Job started"

    }

    $clearJobs = @()
    $clearPowers = @()
    
    $index = 0
    foreach($job in $jobs){
        if($job.IsCompleted){
            $clearJobs += $job
            $clearPowers += $powers[$index]
        }
    }

    $index = 0
    foreach($job in $clearJobs){
        
        $myresult = $powers[$index].endInvoke($job)
        write-host "clearing job: $($myresult)"
        $jobs.remove($job)
        $index+=1
    }

    foreach($power in $clearPowers){
        $power.dispose()
        $powers.remove($power)
    }

    write-host "current jobs"

    write-host $jobs

    write-host "next request"


    $token = $results[$results.Count -1].ContinuationToken;
}
While ($token -ne $Null)

write-host "Total of blobs searched: $($total)"

$index = 0
while ($Jobs.IsCompleted -contains $false) {
    Start-Sleep 30
    write-host "Waiting for jobs..."
    $clearJobs = @()
    foreach($job in $jobs){
        if($job.IsCompleted){
            $clearJobs += $job
            write-host "clearing job: $($job.instanceId)"
        }
        $index+=1
    }

    foreach($job in $clearJobs){
        $jobs.remove($job)
    }

}

write-host "finished"

#cd..

