app.controller(
        "xreffiletoeventController",
        function( $scope, $http, modals ) {
        	
        	
        	$scope.events = [];
        	
        	
            function save(){
            	var data = $scope.events;
        		var config = {headers : {'Content-Type': 'application/json'}};
        		
        		$http.post('/map-api/v1/xreffiles/', data, config).
        		then(
        			function(response) {
        				alert(response.data);
        			}, 
        			function(response) {
        				alert(response.data);
        			}
        		);
            }
            
            
            $scope.saveFilenameXREF = function() {
            		save();
            };
            
            $scope.addItem = function(){
            	$scope.events.push({key:"",event:""});
            }
            
            $scope.removeItem = function(index){
            	$scope.events.splice(index,1);
            }
            
            // load the table
            $http.get('/map-api/v1/xreffiles/').
    		then(
    			function(response) {
   					
    				$scope.events = response.data;
    			}, 
    			function(response) {
    				alert(response.data);
    			}
    		);
            
        }
    );

