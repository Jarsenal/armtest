app.controller(
        "queryController",
        function( $scope, $http, modals ) {
        	
        	
        	function testQuery(){
            	
        		var myconfig = {};
        		
        		for(var item in $scope.sql.config){
        			myconfig[$scope.sql.config[item].key] = $scope.sql.config[item].value
        		}
        		
        		
        		var data = JSON.stringify({
        			event: $scope.query.name,
        			query: $scope.sql.request,
        			fields: $scope.sql.variables,
        			config: myconfig,
        			date: $scope.sql.date
        		});
        		
        		
            	var config = {headers : {'Content-Type': 'application/json'}};
        		
        		$http.post('/map-api/v1/process/query', data, config).
        		then(
        			function(response) {
        				if((typeof response.data) == "string")
        					$scope.result = response.data;
        				else
        					$scope.result = JSON.stringify(response.data, null, 2);
        			}, 
        			function(response) {
        				$scope.data = response.data;
        			}
        		);
            }
        	
        	function getValue(list, key){
            	for(i in list){
            		if(list[i].key = key)
            			return list[i].value;
            	}
            	return "";
            }
            
            function save(name){
            	
        		var data = {
        				query: $scope.sql.request,
        				date: $scope.sql.date,
        				config: {}
        		}
        		
        		for(var item in $scope.sql.config){
        			data.config[$scope.sql.config[item].key] = $scope.sql.config[item].value
        		}
        		
        		
        		var config = {headers : {'Content-Type': 'application/json'}};
        		
        		$http.post('/map-api/v1/queries/' + name, data, config).
        		then(
        			function(response) {
       					$scope.result = response.data;
       					$scope.query.name = name;
        			}, 
        			function(response) {
        				$scope.result = response.data;
        			}
        		);
            }
            
            $scope.query = {
        			name: "New-Query"
        	}
        	$scope.queries = [];
        	$scope.sql = {
        			request: "",
        			variables: [],
        			config: [],
        			date: "yyyy-MM-dd'T'HH:mm:ssZZZ"
        	};
        	
        	$scope.errorMessage = null;
            $scope.cancel = modals.reject;
            $scope.submit = function() {
               return( $scope.errorMessage = "Please provide something!" );
            };
            
            $scope.checkVars = function() {
            	var tempVariables = $scope.sql.variables;
            	$scope.sql.variables = [];
            	var tags = $scope.sql.request.match(/<<([A-Za-z]*)>>/gi);
            	for(i in tags){
        			var val = tags[i].replace("<<","").replace(">>","");
        			var vari = {
    					name: val,
    					type: "STRING",
    					value: getValue(tempVariables, val)
        			}
        			$scope.sql.variables.push(vari);
            	}
            }
            
            // load dropdownlist
            $http.get('/map-api/v1/queries?new=yes', 
    				{}).
    		then(
    			function(response) {
    				$scope.queries = response.data;
    			}, 
    			function(response) {
    				$scope.errorMessage = "There was an error in loading the mappings."
    			}
    		);
            
            $scope.sendQuery = function(){
            	testQuery();
            }
            
            $scope.loadQuery = function() {
                var promise = modals.open(
                    "load",
                    {}
                );
                promise.then(
                    function handleResolve( response ) {
                        console.log( "Prompt resolved with [ %s ].", response );
                        var queryname = response;
                        $http.get('/map-api/v1/queries/' + response).
                		then(
                			function(response) {
                				$scope.sql.request = response.data.query;
                				$scope.sql.config = [];
                				$scope.sql.date = response.data.date;
                				
                				for(var item in response.data.config){
                					$scope.sql.config.push({
                						key: item,
                						value: response.data.config[item]
                					});
                				}
                				
                				$scope.query.name = queryname;
                				$scope.checkVars();
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

            $scope.saveQuery = function() {
            	
            	if($scope.query.name === "New-Query"){
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
            		save($scope.query.name);
        		}
            };
            
            $scope.saveasQuery = function() {
            	
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
            	$scope.sql.config.push({key:"",value:""});
            };
            
            $scope.removeItem = function(index){
            	$scope.sql.config.splice(index,1);
            };
            
        }
    );


app.controller(
        "loadQueryController",
        function( $scope, $http, modals ) {
        	
        	$scope.queries = [];
            $scope.query = {name: ""};
            
            $scope.errorMessage = null;
            $scope.cancel = modals.reject;
            $scope.submit = function() {
                if ($scope.query.name == null || $scope.query.name == "" ) {
                    return( $scope.errorMessage = "Please provide something!" );
                }
                modals.resolve( $scope.query.name );
            };
            
            
            // load dropdownlist
            $http.get('/map-api/v1/queries', 
    				{}).
    		then(
    			function(response) {
    				$scope.queries = response.data;
    			}, 
    			function(response) {
    				$scope.errorMessage = "There was an error in loading the mappings."
    			}
    		);
        }
    );



app.controller(
    "saveQueryController",
    function( $scope, $http, modals ) {
    	$scope.queries = [];
    	$scope.query = {name: modals.params().mapping};
        $scope.errorMessage = null;
        $scope.cancel = modals.reject;
        $scope.submit = function() {
        	
            if ($scope.query.name == null || $scope.query.name == "" ) {
                return( $scope.errorMessage = "Please provide something!" );
            }
            
            modals.resolve( $scope.query.name );
        };
        
        // load dropdownlist
        $http.get('/map-api/v1/queries?new=yes', 
				{}).
		then(
			function(response) {
				$scope.queries = response.data;
			}, 
			function(response) {
				$scope.errorMessage = "There was an error in loading the mappings."
			}
		);
        
    }
);
