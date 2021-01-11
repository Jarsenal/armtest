app.controller(
        "holdsController",
        function( $scope, $http, modals ) {
        	
            function loadHolds(){
                $http.get('/transactions/holds').
            	then(
        			function(response) {
        				$scope.holds = response.data;
        			}, 
        			function(response) {
        				alert("There was an error loading the events");
        			}
        		);
            }
            
            function loadRetries(){
                $http.get('/transactions/retries').
            	then(
        			function(response) {
        				$scope.retries = response.data;
        			}, 
        			function(response) {
        				alert("There was an error loading the events");
        			}
        		);
            }
            
            
            function clearHold(id){
                $http.delete('/transactions/holds/' + id ).
            	then(
        			function(response) {
        				alert(response.data);
        				
        				loadHolds();
        			}, 
        			function(response) {
        				alert("There was an error loading the events");
        			}
        		);
            }
            
            function clearRretry(id){
                $http.delete('/transactions/retries/' + id ).
            	then(
        			function(response) {
        				alert(response.data);
        				
        				loadRetries();
        			}, 
        			function(response) {
        				alert("There was an error loading the events");
        			}
        		);
            }
            
            $scope.holds = [];
            $scope.tretires = [];
            
            $scope.loadData = function() {
            	loadHolds();
                loadRetries();
                
            };
            
            loadHolds();
            loadRetries();
            
        }
    );



