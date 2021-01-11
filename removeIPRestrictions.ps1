param($subscriptionName, $username, $password, $tenant, $resourcegroup)

#login
az login --service-principal --username $username --password $password --tenant $tenant
az account set --subscription """$($subscriptionName)"""

$faList = az functionapp list --resource-group $resourceGroup  --query "[].name" -o tsv
write-host "Function List: $($faList)"
write-host "Resource Group: $($resourcegroup)" 


ForEach ($function in $faList) {
	
	write-host "Removing access restrictions: $($function)"
	
	$rules = (az functionapp config  access-restriction show  -n $function -g $resourcegroup | ConvertFrom-Json)
	
	write-host "Retrieved rules: $($function)"
	
	ForEach ($rule in $rules.ipSecurityRestrictions){
		write-host "Removing: $($rule.name)"
		az functionapp config  access-restriction remove  -n $function -g $resourcegroup -r $rule.name --scm-site false
	}
	
	ForEach ($rule in $rules.scmIpSecurityRestrictions){
		write-host "Removing: $($rule.name)"
		az functionapp config  access-restriction remove  -n $function -g $resourcegroup -r $rule.name --scm-site true
	}
}

exit 0