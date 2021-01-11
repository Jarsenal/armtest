app.controller(
        "schedulestoeventsController",
        function( $scope, $http, modals ) {
        	
        	
        	$scope.schedules = [];
        	
        	
            function save(){
            	var data = $scope.schedules;
        		var config = {headers : {'Content-Type': 'application/json'}};
        		
        		$http.post('/map-api/v1/schedules/', data, config).
        		then(
        			function(response) {
        				alert(response.data);
        			}, 
        			function(response) {
        				alert(response.data);
        			}
        		);
            }
            
            
            $scope.saveConfiguration = function() {
            	save();
            };
            
            
            // load the table
            $http.get('/map-api/v1/schedules/').
    		then(
    			function(response) {
    				$scope.schedules = response.data;
    			}, 
    			function(response) {
    				alert(response.data);
    			}
    		);
        }
    );

