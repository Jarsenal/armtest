
param($org, $env, $region, $subscriptionName, $resourceGroup, $username, $password, $tenant, $vnetPrefix, $subnetList, $sqlUser, $sqlPassword, $secretKey, $storageLocation)



write-host "==== LOGGING INTO AZURE ===="
az login --service-principal --username $username --password $password --tenant $tenant
az account set --subscription """$($subscriptionName)"""



write-host "==== Init VARS ===="
	# for Subscription
		$subscriptionId = az account show -s """$($subscriptionName)""" --query id
		write-host "Subscription NAME, ID = $($subscriptionName), $($subscriptionId)"
		
	# for Keyvault
		$kvName = "kv-$($org)-bib-$($env)-01"

	# for FAs
		$faResource = "/subscriptions/$($subscriptionId)/resourceGroups/$($resourceGroup)/providers/Microsoft.Web/sites"
		$faUI = "asfa-$($org)-bib-$($env)-biibui"
		$faLOGGING = "asfa-$($org)-bib-$($env)-logging"
		$faMAPPING = "asfa-$($org)-bib-$($env)-mapping"
		$faHTTP = "asfa-$($org)-bib-$($env)-routinghttp"
		$faSQ = "asfa-$($org)-bib-$($env)-routingsq"
		$faAMQ = "asfa-$($org)-bib-$($env)-amq"
		$faREC = "asfa-$($org)-bib-$($env)-receive"
		write-host "==== Get App Insights key ===="
		$aiKey =(az resource show -g  "$($resourceGroup)" -n "ai-$org-bib-$env-01" --resource-type microsoft.insights/components | ConvertFrom-Json).properties.InstrumentationKey
		$faList = @($faLOGGING,$faMAPPING,$faHTTP,$faSQ,$faAMQ,$faREC)

	# for LAs
		$laList = (az resource list -g $($resourceGroup) --resource-type Microsoft.Logic/workflows | convertfrom-json).name
		$laIps = @()

	# for Storage
		$primaryStorageName = "salrs$($storageLocation)$($org)bib$($env)0001"
		$primaryStorageId = az storage account show --name $primaryStorageName --resource-group $resourceGroup  --query id --output tsv
		$primaryStorage_primaryKey = az storage account keys list --resource-group $resourceGroup -n $primaryStorageName --query [0].value --output tsv
		$primaryStorageConnection = "DefaultEndpointsProtocol=https;AccountName=$($primaryStorageName);AccountKey=$($primaryStorage_primaryKey);EndpointSuffix=core.windows.net"
		$primaryStorage_receiveQueueName = "biib-receive"
		$primaryStorage_criticalQueueName = "biib-critical"
		$primaryStorage_reprocessQueueName = "biib-reprocess"
		$primaryStorage_receiveQueueId = "$($primaryStorageId)/queueservices/default/queues/$($primaryStorage_receiveQueueName)"
		$primaryStorage_criticalQueueId = "$($primaryStorageId)/queueservices/default/queues/$($primaryStorage_criticalQueueName)"
		$primaryStorage_reprocessQueueId = "$($primaryStorageId)/queueservices/default/queues/$($primaryStorage_reprocessQueueName)"

	# for EGTs
		$receiveQueueTopic_Name = "egs-$($org)-bib-$($env)-receivequeue"
		$receiveQueueTopic = az eventgrid topic show --name $receiveQueueTopic_Name -g $resourceGroup |  ConvertFrom-Json
		$criticalQueueTopic_Name = "egs-$($org)-bib-$($env)-receivecritical"
		$criticalQueueTopic = az eventgrid topic show --name $criticalQueueTopic_Name -g $resourceGroup |  ConvertFrom-Json
		$reprocessQueueTopic_Name = "egs-$($org)-bib-$($env)-receivereprocess"
		$reprocessQueueTopic = az eventgrid topic show --name $reprocessQueueTopic_Name -g $resourceGroup |  ConvertFrom-Json
		$reprocessQueueTopic.endpoint -match 'https://(.*)/api/events'
		$reprocessQueueTopic_HOST = $Matches[1]
		$reprocessQueueTopic_primaryKey = az eventgrid topic key list -n $reprocessQueueTopic_Name -g $resourceGroup --query key1 -o tsv
		$loggingQueueTopic_Name = "egs-$($org)-bib-$($env)-receivelogging"
		$loggingQueueTopic = az eventgrid topic show --name $loggingQueueTopic_Name -g $resourceGroup |  ConvertFrom-Json
		$loggingQueueTopic.endpoint -match 'https://(.*)/api/events'
		$loggingQueueTopic_HOST = $Matches[1]
		$loggingQueueTopic_primaryKey = az eventgrid topic key list -n $loggingQueueTopic_Name -g $resourceGroup --query key1 -o tsv

	# for EventHub
		$eventHubNamespace_Name = "eh-$($org)-bib-$($env)-logs"
		$eventHubDetails = az eventhubs eventhub show --name evh-logs  --resource-group $resourceGroup --namespace-name $eventHubNamespace_Name | ConvertFrom-Json
		$eventHub_ConnectionString = az eventhubs eventhub authorization-rule keys list -g $resourceGroup --namespace-name $eventHubNamespace_Name --eventhub-name "evh-logs" -n "ReadMessages" --query "primaryConnectionString" -o tsv

	# for SQL server
		$sqlServer = "dbs-$($org)-bib-$($env)-logs"
		$sqlHost = "dbs-$($org)-bib-$($env)-logs.database.windows.net"
		$sqlDB = "db-$($org)-bib-logs"

	# for VNET
		$vnetName="vnet-$($org)-bib-$($env)"
		$subnet="default"
		$LocalIP="127.0.0.1/32"	
		$ruleDeny="Local IP"
		$ruleAllow="BiiB all"
		$faSubnetName=""
		$faSubnetId=0
		$nsgName="nsg-assw-$($org)-bib-$($env)-app-01"
		$nsgId = az network nsg list --resource-group $resourceGroup --query "[?name=='$($nsgName)'].id" -o tsv

write-host "=== VARS initialized ==="



write-host "==== START - SUBSCRIBE QUEUE TO TOPIC ===="
	az eventgrid event-subscription create --name "sub-biib-queue" --source-resource-id $receiveQueueTopic.id --endpoint-type storagequeue --endpoint $primaryStorage_receiveQueueId  --deadletter-endpoint "/subscriptions/$($subscriptionId)/resourceGroups/$($resourceGroup)/providers/Microsoft.Storage/storageAccounts/$($primaryStorageName)/blobServices/default/containers/deadletters"
	az eventgrid event-subscription create --name "sub-biib-critical" --source-resource-id $criticalQueueTopic.id --endpoint-type storagequeue --endpoint $primaryStorage_criticalQueueId --deadletter-endpoint "/subscriptions/$($subscriptionId)/resourceGroups/$($resourceGroup)/providers/Microsoft.Storage/storageAccounts/$($primaryStorageName)/blobServices/default/containers/deadletters"
	az eventgrid event-subscription create --name "sub-biib-reprocess" --source-resource-id $reprocessQueueTopic.id --endpoint-type storagequeue --endpoint $primaryStorage_reprocessQueueId --deadletter-endpoint "/subscriptions/$($subscriptionId)/resourceGroups/$($resourceGroup)/providers/Microsoft.Storage/storageAccounts/$($primaryStorageName)/blobServices/default/containers/deadletters"
	az eventgrid event-subscription create --name "sub-biib-logging" --source-resource-id $loggingQueueTopic.id --endpoint-type eventhub --endpoint $eventHubDetails.id --deadletter-endpoint "/subscriptions/$($subscriptionId)/resourceGroups/$($resourceGroup)/providers/Microsoft.Storage/storageAccounts/$($primaryStorageName)/blobServices/default/containers/deadletters"
write-host "====  END - SUBSCRIBE QUEUE TO TOPIC ===="

write-host " === START - CORS ==="
	$primaryStorage_primaryStaticWebEndpoint = (az storage account show -g $resourceGroup  -n $primaryStorageName --query primaryEndpoints.web).replace("net/","net")
	az functionapp cors remove --resource-group "$($resourceGroup)"  --name "$($faUI)"  --allowed-origins "$($primaryStorage_primaryStaticWebEndpoint)"
	az functionapp cors add --resource-group "$($resourceGroup)"  --name "$($faUI)"  --allowed-origins "$($primaryStorage_primaryStaticWebEndpoint)"
write-host " ===  END - CORS ==="

write-host " === START - FAs ==="
	# FA MAPPING
		az functionapp config appsettings set --name "$($faMAPPING)" --resource-group "$($resourceGroup)" --settings "BLOBSTORAGE=$($primaryStorageConnection)"
		az functionapp config appsettings set --name "$($faMAPPING)" --resource-group "$($resourceGroup)" --settings "storageAccountKey=$($primaryStorage_primaryKey)"
		az functionapp config appsettings set --name "$($faMAPPING)" --resource-group "$($resourceGroup)" --settings "storageAccountName=$($primaryStorageName)"
		az functionapp config appsettings set --name "$($faMAPPING)" --resource-group "$($resourceGroup)" --settings "APPINSIGHTS_INSTRUMENTATIONKEY=$($aiKey)"
		az functionapp config appsettings set --name "$($faMAPPING)" --resource-group "$($resourceGroup)" --settings "EventHub=$($eventHub_ConnectionString)"

	# FA RECEIVE
		az functionapp config appsettings set --name "$($faREC)" --resource-group "$($resourceGroup)" --settings "BLOBSTORAGE=$($primaryStorageConnection)"
		az functionapp config appsettings set --name "$($faREC)" --resource-group "$($resourceGroup)" --settings "APPINSIGHTS_INSTRUMENTATIONKEY=$($aiKey)"
		az functionapp config appsettings set --name "$($faREC)" --resource-group "$($resourceGroup)" --settings "EventHub=$($eventHub_ConnectionString)"

	# FA SQ Routing
		az functionapp config appsettings set --name "$($faSQ)" --resource-group "$($resourceGroup)" --settings "BLOBSTORAGE=$($primaryStorageConnection)"
		az functionapp config appsettings set --name "$($faSQ)" --resource-group "$($resourceGroup)" --settings "SECRET_KEY=$($secretkey)"
		az functionapp config appsettings set --name "$($faSQ)" --resource-group "$($resourceGroup)" --settings "APPINSIGHTS_INSTRUMENTATIONKEY=$($aiKey)"
		az functionapp config appsettings set --name "$($faSQ)" --resource-group "$($resourceGroup)" --settings "EventHub=$($eventHub_ConnectionString)"

	# FA HTTP Routing
		az functionapp config appsettings set --name "$($faHTTP)" --resource-group "$($resourceGroup)" --settings "BLOBSTORAGE=$($primaryStorageConnection)"
		az functionapp config appsettings set --name "$($faHTTP)" --resource-group "$($resourceGroup)" --settings "SECRET_KEY=$($secretkey)"
		az functionapp config appsettings set --name "$($faHTTP)" --resource-group "$($resourceGroup)" --settings "APPINSIGHTS_INSTRUMENTATIONKEY=$($aiKey)"
		az functionapp config appsettings set --name "$($faHTTP)" --resource-group "$($resourceGroup)" --settings "EventHub=$($eventHub_ConnectionString)"

	# FA BIIBUI
		az functionapp config appsettings set --name "$($faUI)" --resource-group "$($resourceGroup)" --settings "BLOBSTORAGE=$($primaryStorageConnection)"
		az functionapp config appsettings set --name "$($faUI)" --resource-group "$($resourceGroup)" --settings "SECRET_KEY=$($secretkey)"
		az functionapp config appsettings set --name "$($faUI)" --resource-group "$($resourceGroup)" --settings "MAPPING_URL=https://$($faMAPPING).azurewebsites.net"
		az functionapp config appsettings set --name "$($faUI)" --resource-group "$($resourceGroup)" --settings "TRANSACTION_URL=https://$($faLOGGING).azurewebsites.net"
		az functionapp config appsettings set --name "$($faUI)" --resource-group "$($resourceGroup)" --settings "APPINSIGHTS_INSTRUMENTATIONKEY=$($aiKey)"
	
	# FA LOGGING
		az functionapp config appsettings set --name "$($faLOGGING)" --resource-group "$($resourceGroup)" --settings "BLOBSTORAGE=$($primaryStorageConnection)"
		az functionapp config appsettings set --name "$($faLOGGING)" --resource-group "$($resourceGroup)" --settings "EVENT_HOST=$($reprocessQueueTopic_HOST)"
		az functionapp config appsettings set --name "$($faLOGGING)" --resource-group "$($resourceGroup)" --settings "EVENT_KEY=$($reprocessQueueTopic_primaryKey)"
		az functionapp config appsettings set --name "$($faLOGGING)" --resource-group "$($resourceGroup)" --settings "SQL_DB=$($sqlDB)"
		az functionapp config appsettings set --name "$($faLOGGING)" --resource-group "$($resourceGroup)" --settings "SQL_HOST=$($sqlHost)"
		az functionapp config appsettings set --name "$($faLOGGING)" --resource-group "$($resourceGroup)" --settings "SQL_USER=$($sqlUser)"
		az functionapp config appsettings set --name "$($faLOGGING)" --resource-group "$($resourceGroup)" --settings "SQL_PWD=$($sqlPassword)"
		az functionapp config appsettings set --name "$($faLOGGING)" --resource-group "$($resourceGroup)" --settings "EventHub=$($eventHub_ConnectionString)"
		az functionapp config appsettings set --name "$($faLOGGING)" --resource-group "$($resourceGroup)" --settings "storageAccountKey=$($primaryStorage_primaryKey)"
		az functionapp config appsettings set --name "$($faLOGGING)" --resource-group "$($resourceGroup)" --settings "storageAccountName=$($primaryStorageName)"
		az functionapp config appsettings set --name "$($faLOGGING)" --resource-group "$($resourceGroup)" --settings "APPINSIGHTS_INSTRUMENTATIONKEY=$($aiKey)"

	# FA AMQ
		az functionapp config appsettings set --name "$($faAMQ)" --resource-group "$($resourceGroup)" --settings "ACTIVEMQ_HOST="
		az functionapp config appsettings set --name "$($faAMQ)" --resource-group "$($resourceGroup)" --settings "ACTIVEMQ_PASSWORD="
		az functionapp config appsettings set --name "$($faAMQ)" --resource-group "$($resourceGroup)" --settings "ACTIVEMQ_USER="
		az functionapp config appsettings set --name "$($faAMQ)" --resource-group "$($resourceGroup)" --settings "BLOBSTORAGE=$($primaryStorageConnection)"
		az functionapp config appsettings set --name "$($faAMQ)" --resource-group "$($resourceGroup)" --settings "QUEUE_TIMEOUT="
		az functionapp config appsettings set --name "$($faAMQ)" --resource-group "$($resourceGroup)" --settings "SECRET_KEY=$($secretkey)"
		az functionapp config appsettings set --name "$($faAMQ)" --resource-group "$($resourceGroup)" --settings "URL_TARGET_QUEUE="
		az functionapp config appsettings set --name "$($faAMQ)" --resource-group "$($resourceGroup)" --settings "URL_TARGET_QUEUE_CRITICAL="
		az functionapp config appsettings set --name "$($faAMQ)" --resource-group "$($resourceGroup)" --settings "URL_TARGET_STREAM="
		az functionapp config appsettings set --name "$($faAMQ)" --resource-group "$($resourceGroup)" --settings "APPINSIGHTS_INSTRUMENTATIONKEY=$($aiKey)"
		az functionapp config appsettings set --name "$($faAMQ)" --resource-group "$($resourceGroup)" --settings "EventHub=$($eventHub_ConnectionString)"
		#hardcode these methods to disabled, will make dynamic for SIMA if host is being passed
		az functionapp config appsettings set --name "$($faAMQ)" --resource-group "$($resourceGroup)" --settings "AzureWebJobs.activemq-jms-queue-schedule-critical.Disabled=true"
		az functionapp config appsettings set --name "$($faAMQ)" --resource-group "$($resourceGroup)" --settings "AzureWebJobs.activemq-jms-queue-schedule-rapid.Disabled=true"
		az functionapp config appsettings set --name "$($faAMQ)" --resource-group "$($resourceGroup)" --settings "AzureWebJobs.activemq-jms-queue-schedule-batch.Disabled=true"
write-host " === END - FAs ==="


write-host " === START - LAs ==="
	$metrics =  ('[{"category": "AllMetrics","enabled": true,"retentionPolicy": {"enabled": false,"days": 0}}]' ) | ConvertTo-Json
	$logs = ('[{"category": "WorkflowRuntime","enabled": true,"retentionPolicy": {"enabled": false,"days": 0}}]') | ConvertTo-Json

	foreach($la in $laList) {
		write-host "Creating logging for $($la)"
		$laId = (az resource show -g "$($resourceGroup)" -n "$($la)" --resource-type "Microsoft.Logic/workflows" --query id --output tsv)
		az monitor diagnostic-settings create --resource $laId -g "$($resourceGroup)" -n "logging" --workspace "law-$($org)-bib-$($env)-lamonitor" --logs $logs --metrics $metrics
		write-host "Finish logging for $($la)"
		
		write-host "Getting IPS for $($la)"				 
		$laiplist = (az resource show -g  "$($resourceGroup)" -n $la --resource-type Microsoft.Logic/workflows --query properties.endpointsConfiguration.workflow.outgoingIpAddresses | ConvertFrom-Json)
		foreach($item in $laiplist) {
			$laIps += $item.address
		}
	}
	$laIps = ($laIps | select -Unique)
	write-host "LAs Unique ip list: $($laIps)"
write-host " === END - LAs ==="


write-host "==== START - SECURITY ===="
	# for VNET
		$existingVnet = az network vnet show -g "$($resourceGroup)" -n $($vnetName) --query "name" -o tsv
		if(-not $existingVnet) {
			write-host "==== Creating Vnet $($vnetName) ===="
			az network vnet create -g "$($resourceGroup)" -n "$($vnetName)" --address-prefix $vnetPrefix
		}
		
		foreach($_subnetInfo in $subnetList.split(';')) {
			write-host $_subnetInfo
			$_subnetDetails = $_subnetInfo.split(',')
			$_snet = $_subnetDetails[0]
			$_srange = $_subnetDetails[1]
			$_isFunctionsSnet = $_subnetDetails[2]
			
			$existingSnet = az network vnet show -g "$($resourceGroup)" -n "$($vnetName)" --query "subnets[?contains(name, '$($_snet)')].name" -o tsv
			if(-not $existingSnet) {
				write-host "==== Creating Vnet $($vnetName) Subnet $($_snet) ===="
				az network vnet subnet create -g "$($resourceGroup)"   --name "$($_snet)" --vnet-name "$($vnetName)" --address-prefixes $_srange
			} else {
				write-host "==== Updating Vnet $($vnetName) Subnet $($_snet) ===="
				az network vnet subnet update -g "$($resourceGroup)"   --name "$($_snet)" --vnet-name "$($vnetName)" --address-prefixes $_srange
			}
			
			if($_isFunctionsSnet -eq 'Y') {
				$faSubnetName = $_snet;
				$faSubnetId = az network vnet show -g "$($resourceGroup)" -n "$($vnetName)" --query "subnets[?contains(name, '$($_snet)')].id" -o tsv
			}
		}
		
		if($faSubnetName -eq '') {
			Write-Error 'No subnet was marked as application subnet, check your pipeline variables subnetList! Cannot proceed!' -ErrorAction Stop
		}

	# for FAs
		az functionapp vnet-integration add -g "$($resourceGroup)" -n "$($faREC)" --vnet "$($vnetName)" --subnet "$($faSubnetName)" 
		az functionapp vnet-integration add -g "$($resourceGroup)" -n "$($famapping)" --vnet "$($vnetName)" --subnet "$($faSubnetName)" 
		az functionapp vnet-integration add -g "$($resourceGroup)" -n "$($falogging)" --vnet "$($vnetName)" --subnet "$($faSubnetName)" 
		az functionapp vnet-integration add -g "$($resourceGroup)" -n "$($faUI)" --vnet "$($vnetName)" --subnet "$($faSubnetName)" 
		az functionapp vnet-integration add -g "$($resourceGroup)" -n "$($faHTTP)" --vnet "$($vnetName)" --subnet "$($faSubnetName)" 
		az functionapp vnet-integration add -g "$($resourceGroup)" -n "$($faSQ)" --vnet "$($vnetName)" --subnet "$($faSubnetName)"
		az functionapp vnet-integration add -g "$($resourceGroup)" -n "$($faAMQ)" --vnet "$($vnetName)" --subnet "$($faSubnetName)"

		write-host " === Configuring FAs Access ==== "
		ForEach ($fa in $faList) {
			write-host "Setting access for $($fa)"
			$functionDetails = az functionapp show --name $($fa) --resource-group $resourceGroup | ConvertFrom-Json
			# need to implement a way to pass a list of ips of the support's local ip to access this endpoint
			az functionapp config  access-restriction add --rule-name $ruleDeny --priority 250 --action Allow --ids $functionDetails.id --ip-address $LocalIP --scm-site true
			az functionapp config  access-restriction add --rule-name $ruleAllow --priority 300 --action Allow --ids $functionDetails.id --subnet $faSubnetId 
			$netStart = 350
			
			# !!! we are white-listing here all logic apps withing this azure reagion, as all LAs in a reagion use same set of ips !!!
			# also, we need to find a way to batch this as this is the longest lasting operation in entire deployment
			$laIps | ForEach-Object {
				$netStart = $netStart + 1
				$ruleName = "LogicApp-" + $netStart
				az functionapp config  access-restriction add --rule-name $ruleName --priority $netStart --action Allow --ids $functionDetails.id --ip-address  "$($_)/32"
			}
		}

	# for SQL Server
		az network vnet subnet update --ids $faSubnetId --service-endpoints "Microsoft.Web" "Microsoft.Sql"
		az sql server firewall-rule delete --name AllowAllWindowsAzureIps --resource-group $resourceGroup --server $sqlServer
		az sql server vnet-rule create --name BiiBVnet  --resource-group $resourceGroup --server $sqlServer --subnet $faSubnetId
		
	# for NSGs
		#az network vnet subnet update
		if($nsgId) {
			write-host "NSG $($nsgName) exists. NSG ID is $($nsgId). Cleaning up..."
			
			$sidList = az network nsg show --id $nsgId --query subnets[].id -o tsv
			ForEach ($sid in $sidList) {
				#az network nsg update --id $nsgId --remove 
				write-host " Remove $($sid) from $($nsgName)"
				az network vnet subnet update --id $sid --network-security-group """"""
			}
			
			$ruleList = az network nsg show --id $nsgId --query securityRules[].id -o tsv
			ForEach ($rule in $ruleList) {
				write-host " Remove $($rule) from $($nsgName)"
				az network nsg rule delete --id $rule
			}
		} else {
			write-host "Did not find NSG, creating $($nsgName). Defult security rules will be applied."
			az network nsg create -g $resourceGroup -n $nsgName
		}

		foreach($_snetInfo in $subnetList.split(';')) {
			az network vnet subnet update -g "$($resourceGroup)" --vnet-name $($vnetName) -n $_snetInfo.split(',')[0] --network-security-group "$nsgName"
		}

		az network nsg rule create -g "$($resourceGroup)" --nsg-name "$nsgName" -n "InboundHTTPSAllow" --priority 100 --protocol * --direction "Inbound" --destination-port-ranges 443 --destination-address-prefixes "VirtualNetwork" --access "Allow"
		az network nsg rule create -g "$($resourceGroup)" --nsg-name "$nsgName" -n "InboundAPIMManageAllow" --priority 110 --protocol * --direction "Inbound" --destination-port-ranges 3443 --destination-address-prefixes "VirtualNetwork" --access "Allow"
		az network nsg rule create -g "$($resourceGroup)" --nsg-name "$nsgName" -n "OutboundHTTPSAllow" --priority 100 --protocol * --direction "Outbound" --destination-port-ranges 443 --access "Allow"

write-host "==== END - SECURITY ===="

write-host "=====  END ====="