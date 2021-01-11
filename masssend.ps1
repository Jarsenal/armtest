param(
    [string] $dir = "c:/output/jobresults/transactions.1.txt",
    [string] $event = "sap-settled-invoice", 
    [int32] $size = 200
	  )

$url="https://<host>/api/transactions/reprocess?code=<code>"

[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls11

$count=0
$payload=""

$headers = @{
	"Accept"="application/json"
	"Content-Type"="application/json"
}

foreach($line in [System.IO.File]::ReadLines("$($dir)")) {
    
  write-host "reading line $($line)"
    if($line -eq ""){
      continue
    }
    
    if($count -eq 0){
      $payload = "["
    }
    
    
    if(-Not ($count -eq 0)){
      $payload = $payload + ","
    }
    
    $count = $count + 1
    

    $payload = $payload + "{""event"":""$($event)"",""transaction"":""$($line)""}"
    
    
    if($count -eq $size){
      
      $payload = $payload + "]"
    
      write-host $payload
      Invoke-RestMethod -Method Post -Uri $url -Headers $headers -Body $payload -ContentType 'application/json'
      $count = 0
      $payload = ""
  }
}

write-host "Leftover count $($count)"

if($count -gt 0){
  write-host "did not fall into here?"
  $payload = $payload + "]"
    
  write-host $payload
  
  write-host $url

  Invoke-RestMethod -Method Post -Uri $url -Headers $headers -Body $payload -ContentType 'application/json'
  
}
