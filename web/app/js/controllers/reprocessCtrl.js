app.controller(
        "reprocessController",
        function( $scope, $http, modals ) {
        	
            function myloadEvents(){
                $http.get('/transactions/reprocess').
            	then(
        			function(response) {
        				$scope.events = [];
        				
        				for(var ev in response.data){
        					$scope.events.push(
								{
									name: response.data[ev],
									transactions: []
								}
							)
        				}
        			},
            	 
    			function(response) {
    				alert("There was an error loading the events");
    			}
        	);
            }
            
            $scope.events = [];
            
            $scope.loadEvents = function() 
            {
            	myloadEvents();
            };

            $scope.runTransaction = function(event, transaction){
            	var promise = modals.open(
                        "reprocess",
                        {
                        	event: event,
                        	transaction: transaction
                        }
                    );
                    promise.then(
                        function handleResolve( response ) {
                        	$http.get('/transactions/reprocess/' + response.event + "/" + response.id).
                        	then(
                    			function(response) {
                    				alert("Processing");
                    			},
                        	 
            	    			function(response) {
            	    				alert("There was an error loading the events");
            	    			}
                        	);
                            },
                        function handleReject( error ) {
                            console.warn( "Prompt rejected!" );
                        }
                    );
            }
            
            $scope.loadTransaction = function(index){
            	var event = $scope.events[index].name;
            	$http.get('/transactions/reprocess/' + event).
            	then(
        			function(response) {
        				$scope.events[index].transactions = [];
        				for(var ev in response.data){
        					$scope.events[index].transactions.push(
								{
									id: response.data[ev].key,
									modified: response.data[ev].modified
								}
							)
        				}
        			},
            	 
	    			function(response) {
	    				alert("There was an error loading the events");
	    			}
            	);
            
            }
            
            myloadEvents();
            
        }
    );


app.controller(
	    "reprocessTransactionController",
	    function( $scope, $http, modals ) {
	    	$scope.transaction = {
	    			event: modals.params().event,
	    			id: modals.params().transaction
	    		};
	        $scope.errorMessage = null;
	        $scope.cancel = modals.reject;
	        $scope.submit = function() {
	            modals.resolve($scope.transaction);
	        };
	           
	    }
	);
