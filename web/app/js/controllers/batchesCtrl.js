app.controller(
        "batchesController",
        function( $scope, $http, modals ) {
        	
            function myloadBatches(){
                $http.get('/transactions/batches').
            	then(
        			function(response) {
        				$scope.batches = response.data;
        			}, 
        			function(response) {
        				alert("There was an error loading the events");
        			}
        		);
            }
            
            function clearBatches(){
                $http.delete('/transactions/batches').
            	then(
        			function(response) {
        				alert(response.data);
        				
        				myloadBatches();
        			}, 
        			function(response) {
        				alert("There was an error loading the events");
        			}
        		);
            }
            
            $scope.batches = [];
            
            $scope.loadBatches = function() {
            	myloadBatches();
            };
            
            $scope.deleteBatches = function() {
            	clearBatches();
            }

            
            myloadBatches();
            
        }
    );



