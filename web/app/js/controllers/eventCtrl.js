app.controller(
        "eventController",
        function( $scope, $http, modals ) {
        	
        	function save(){
        		
            	for(event in $scope.events){
            		$scope.events[event].edit = false;
            	}
            	
            	var data = $scope.events;
        		var config = {
					headers : {'Content-Type': 'text/plain'},
					params : {'code':$scope.biibKey}
				};
        		
        		$http.post($scope.apiHost + '/configs/events/', data, config).
        		then(
        			function(response) {
       					alert("Success!")
        			}, 
        			function(response) {
        				alert("Failed!");
        			}
        		);
            }
            
            function myloadEvents(){
				
				var config = { params : {'code':$scope.biibKey}};
        		
				$http.get($scope.apiHost + '/configs/events/',config).
            	then(
        			function(response) {
						$scope.events = [];
						for(let i in response.data){
							$scope.events.push(
								{
									name:response.data[i].name,
									duplicates:response.data[i].duplicates,
									errors:response.data[i].errors,
									follows:response.data[i].follows,
									changes:response.data[i].changes?response.data[i].changes:[],
									type: response.data[i].type
								}
							)
						}

						$scope.events.sort(
							(a,b) => (a.name > b.name ? 1 : 
								(b.name > a.name ? -1 : 0))
						)
        			}, 
        			function(response) {
        				alert("There was an error loading the events");
        			}
        		);
			}
			
			function updateEvent(index){
            	
            	$scope.eventList = [];
            	for(event in $scope.events){
					var _event = $scope.events[event];
					if(event != index)
            			$scope.eventList.push(_event.name)
            	}
            	
            }
            
            $scope.events = [];
            $scope.eventList = [];
        	
            $scope.loadEvents = function() {
            	myloadEvents();
            };

            $scope.saveEvents = function() {
	            save();
            };
            
            $scope.addEvent = function(){
				for(event in $scope.events){
            		$scope.events[event].edit = false;
				}
				
				updateEvent($scope.events.length);
            	
            	$scope.events.push({
					name: "NewEvent",
					duplicates: [],
					errors: [],
					follows: [],
					changes: [],
					edit: true,
					type: 'JSON'
				});
				
            }
            
            $scope.removeEvent = function(index){
            	$scope.events.splice(index,1);
            }
            
            $scope.editEvent = function(index){
				updateEvent(index);

				for(event in $scope.events){
            		$scope.events[event].edit = false;
            	}
				$scope.events[index].edit = true;
				
			}
			
			$scope.addDuplicate = function(duplicate, index){
				$scope.events[index].duplicates.push({
					name: duplicate,
					when: []
				});
            }
            
            $scope.removeDuplicate = function(event, index){
            	event.duplicates.splice(index,1);
			}
			
			$scope.addChange = function(change, index){
				$scope.events[index].changes.push({
					name: change,
					when: []
				});
            }
            
            $scope.removeChange = function(event, index){
            	event.changes.splice(index,1);
            }
            
            
            $scope.addError = function(error, index){
            	$scope.events[index].errors.push(error);
            }
            
            $scope.removeError = function(event, index){
            	event.errors.splice(index,1);
            }
            
            $scope.addFollow = function(follow, index){
            	$scope.events[index].follows.push(follow);
            }
            
            $scope.removeFollow = function(event, index){
            	event.follows.splice(index,1);
			}
			
			$scope.editCondition = function(_node){
    	
				var promise = modals.open(
						"whenlarge",
						{nodes: _node.when}
					);
					promise.then(
						function handleResolve( response ) {
							_node.when = response;
						},
						function handleReject( error ) {
							console.warn( "Prompt rejected!" );
						}
					);
			}
			
			
			
			myloadEvents();
            
        }
    );


	app.controller(
	    "editEventWhenController",
	    function( $scope, $http, modals ) {
	    	
	    	$scope.node = {
	    			nodes: modals.params().nodes
	    	}
	    	
	    	
	    	$scope.errorMessage = null;
	        $scope.cancel = modals.reject;
	        
	        $scope.save = function() {
	            modals.resolve( $scope.node.nodes );
	        };
	        
	        $scope.addChild = function(nodes) {
	        	nodes.push({
	        		action:"STATEMENT",
        			loop:"",
        			left: "",
        			type: "STRING",
        			op: "EQUALS",
        			right: "",
        			nodes: []
	        	})
	        };
	        
	        $scope.removeChild = function(nodes, index) {
	        	nodes.splice(index,1);
	        }
	        
	        $scope.addRecord = function(nodes){
				if(nodes == null) nodes = [];
	        	nodes.push({
	        		action:"STATEMENT",
        			loop:"",
        			left: "",
        			type: "STRING",
        			leftDateFormat: "",
        			op: "EQUALS",
        			right: "",
        			rightDateFormat: "",
        			nodes: []
	        	})
	        }
	        
	        $scope.removeRecord = function(nodes, index) {
	        	nodes.splice(index,1);
	        }
	    }
	);



