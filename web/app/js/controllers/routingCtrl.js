app.controller(
        "routingController",
        function( $scope, $http, modals ) {
        	
        	
        	function getValue(list, key){
            	for(i in list){
            		if(list[i].key = key)
            			return list[i].value;
            	}
            	return "";
            }
            
            function save(name){
            	
            	
        		var data = {};
        		data['type'] = $scope.routings.type;
        		data['_encrypt'] = [];
        		
        		for(item in $scope.routings.list){
        			data[$scope.routings.list[item].key]=$scope.routings.list[item].value;
        			
        			if($scope.routings.list[item].encrypt){
        				data['_encrypt'].push($scope.routings.list[item].key);
        			}
        		}
        		
        		
            	var config = {
							headers : {'Content-Type': 'text/plain'},
							params : {'code':$scope.biibKey}
						};
        		
        		$http.post($scope.apiHost + '/configs/routings/' + name, data, config).
        		then(
        			function(response) {
       					
        				$scope.routing.name = name;
        			}, 
        			function(response) {
        				alert(response.data);
        			}
        		);
            }
            
            $scope.routing = {
        			name: "New-Route"
        	}
            $scope.routings = {
            	type: "salesforce",
        		list: []
        	}
            
        	$scope.errorMessage = null;
            $scope.cancel = modals.reject;
            $scope.submit = function() {
               return( $scope.errorMessage = "Please provide something!" );
            };
            
            
            $scope.loadRouting = function() {
                var promise = modals.open(
                    "load",
                    {}
                );
                
                promise.then(
                    function handleResolve( response ) {
                        console.log( "Prompt resolved with [ %s ].", response );
						var routingname = response;
						
						var config = {params : {'code':$scope.biibKey}};

                        $http.get($scope.apiHost + '/configs/routings/' + response, config).
                		then(
                			function(response) {
                				
                				var encrypt = response.data['_encrypt'];
                				if(encrypt == null)
                					encrypt = [];
                				
                				$scope.routings.list = [];
                				for(var prop in response.data){
                					if(prop == "type"){
                						$scope.routings.type = response.data[prop]; 
                					}
                					else if(prop == "_encrypt"){
                						continue;
                					}
                					else{
	                					$scope.routings.list.push({
	                						key: prop,
	                						value: response.data[prop],
	                						encrypt: (encrypt.indexOf(prop) >= 0)
	                					});
                					}
                				}
                				$scope.routing.name = routingname;
                				
                			}, 
                			function(response) {
                				alert("There was an error loading the query");
                			}
                		);
                        
                        
                    },
                    function handleReject( error ) {
                        console.warn( "Prompt rejected!" );
                    }
                );
            };

            $scope.saveRouting = function() {
            	
            	if($scope.routing.name === "New-Route"){
        	        var promise = modals.open(
	        	            "save",
	        	            {mapping: ""}
        	        	);
        	        promise.then(
        	            function handleResolve( response ) {
        	                console.log( "Save map to [ %s ].", response );
        	                save(response);
        	            },
        	            function handleReject( error ) {
        	                console.warn( "Prompt rejected!" );
        	            }
        	        );
            	}
            	else
        		{
            		save($scope.routing.name);
        		}
            };
            
            $scope.saveasRouting = function() {
            	
        	    var promise = modals.open(
        	            "save",
        	            {mapping: ""}
    	        	);
    	        promise.then(
    	            function handleResolve( response ) {
    	                console.log( "Save map to [ %s ].", response );
    	                save(response);
    	            },
    	            function handleReject( error ) {
    	                console.warn( "Prompt rejected!" );
    	            }
    	        );
            	
            };
            
            $scope.addItem = function(){
            	$scope.routings.list.push({key:"",value:"",encrypt:false});
            }
            
            $scope.removeItem = function(index){
            	$scope.routings.list.splice(index,1);
            }
            
            $scope.typeSwitch = function(){
            	var requiredFields = {
						//cosmodb: ['database','collection','partitionkey'],
						//cosmodb_bulk: ['database','collection','partitionkey']
            			// salesforce:['external','object'],
            			// salesforce_bulk:['external','object'],
            			// ebmobile_staging:['host','port','path','token.path'],
            			// ftp:['path','name'],
            			// sftp:['path','name'],
						//http:['secure','host','port','user','password','method','path','header:*'] //,
						http:['url','username','password','mimeType','charset','query.*','header.*'],
						activemq:['queueName'],
						blobstorage:['name','folder','action'],
						storagequeue:['accountName','accountKey','queueName'],
						// sap_bapi:['object'],
            			// sap_idoc:['object'],
            			// smtp:['contentType','to','from','subject'],
            			// s3:['accessKey','secretKey','bucket','objectKey']
            	}
            	
            	var fields = requiredFields[$scope.routings.type.replace('-','_')];
            	
            	if(fields != null){
            	
            		for(var index = $scope.routings.list.length - 1; index >= 0; index--){
            			if($scope.routings.list[index].value == "")
            				 $scope.removeItem(index);
            		}
            		
            		for(f in fields){
            			var add = true;
            		
            			for(item in $scope.routings.list){
                			add = (add && fields[f] != $scope.routings.list[item].key);
                		}
                		
            			if(add){
            				$scope.routings.list.push({key:fields[f],value:""});
            			}
            			
            		}
            		
            		
            	}
            	
            }
            
        }
    );


app.controller(
        "loadRoutingController",
        function( $scope, $http, modals ) {
        	
        	$scope.routings = [];
            $scope.route = {name: ""};
            
            $scope.errorMessage = null;
            $scope.cancel = modals.reject;
            $scope.submit = function() {
                if ($scope.route.name == null || $scope.route.name == "" ) {
                    return( $scope.errorMessage = "Please provide something!" );
                }
                modals.resolve( $scope.route.name );
            };
            
            
            // load dropdownlist
            var config = {params : {'code':$scope.biibKey}};
        		
			$http.get($scope.apiHost + '/configs/events/',config).
    		then(
    			function(response) {
					$scope.routings = [];
					for(item in response.data){$scope.routings.push(response.data[item].name);}
    			}, 
    			function(response) {
    				$scope.errorMessage = "There was an error in loading the mappings."
    			}
    		);
        }
    );



app.controller(
    "saveRoutingController",
    function( $scope, $http, modals ) {
    	$scope.routings = [];
    	$scope.route = {name: modals.params().mapping};
        $scope.errorMessage = null;
        $scope.cancel = modals.reject;
        $scope.submit = function() {
        	
            if ($scope.route.name == null || $scope.route.name == "" ) {
                return( $scope.errorMessage = "Please provide something!" );
            }
            
            modals.resolve( $scope.route.name );
        };
        
        // load dropdownlist
		var config = {params : {'code':$scope.biibKey}};
        		
		$http.get($scope.apiHost + '/configs/events/',config).
		then(
			function(response) {
				$scope.routings = [];
				for(item in response.data){$scope.routings.push(response.data[item].name);}
			}, 
			function(response) {
				$scope.errorMessage = "There was an error in loading the mappings."
			}
		);
        
    }
);
