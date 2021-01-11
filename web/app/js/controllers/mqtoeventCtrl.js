app.controller(
        "mqtoeventController",
        function( $scope, $http, modals ) {
        	
        	$scope.events = [];
        	
            function save(){
            	var data = $scope.events;
        		var config = {
					headers : {'Content-Type': 'application/json'},
					params : {'code':$scope.biibKey}
				};
        		
        		$http.post($scope.apiHost + '/configs/mqevents/', data, config).
        		then(
        			function(response) {
        				alert(response.data);
        			}, 
        			function(response) {
        				alert(response.data);
        			}
        		);
			}
			
			function load(){

				var config = {
					params : {'code':$scope.biibKey}
				};

				$http.get($scope.apiHost + '/configs/mqevents/',config).
				then(
					function(response) {
						$scope.events = response.data;
					}, 
					function(response) {
						alert(response.data);
					}
				);

				$http.get($scope.apiHost + '/configs/events/',config).
            	then(
        			function(response) {
						$scope.eventlist = [];
						for(let i in response.data){
							$scope.eventlist.push(response.data[i].name);
						}

						$scope.eventlist.sort(
							(a,b) => (a > b ? 1 : (b > a ? -1 : 0))
						)
        			}, 
        			function(response) {
        				alert("There was an error loading the events");
        			}
        		);
            }
            
            
            
            $scope.saveFile = function() {
            	save();
			};
			
			$scope.loadFile = function() {
				load();
			};

            $scope.addItem = function(){
            	$scope.events.push({key:"",event:""});
            }
            
            $scope.removeItem = function(index){
            	$scope.events.splice(index,1);
            }
            
			load();
            
        }
    );


