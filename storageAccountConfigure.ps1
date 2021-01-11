param($subscriptionName, $username, $password, $tenant, $storageAccount, $webDir)

write-host "==== LOGGING INTO AZURE ===="
az login --service-principal --username $username --password $password --tenant $tenant
az account set --subscription """$($subscriptionName)"""

#create queues
$queues=("biib-receive","biib-critical","biib-reprocess","biib-mapping-batch","biib-mapping-critical","biib-mapping-reprocess")

ForEach($queue in $queues) {
	write-host "start -> $($queue) to $($storageAccount)"
	az storage queue create --account-name $storageAccount -n $queue
	write-host "end -> $($queue)"
}

#create static website
write-host "Create static website"
az storage blob service-properties update --account-name $storageAccount --static-website  --index-document app\index.html

# upload web content
#write-host "Upload web content"
#az storage blob upload-batch --account-name $storageAccount  -d '$web' -s """$($webDir)""" --pattern "*.*"

write-host "==== END ===="


