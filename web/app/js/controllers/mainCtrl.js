app.controller('mainCtrl', function($scope, $http,modals) {
	$scope.biibKey = '';
	$scope.apiHost = 'http://localhost:7071/api';
	$scope.title = "LOCAL";

	$scope.updatekey = function(){
		var promise = modals.open("updatemykey",{});
	promise.then(
		function handleResolve( response ) {
			console.log( "Updated key.");
			$scope.biibKey = response;
		},
		function handleReject( error ) {
			console.warn( "Key Prompt rejected!" );
		}
	);
	}
});


app.controller(
    "updatekey",
    function( $scope, $http, modals ) {
    	$scope.errorMessage = null;
        $scope.cancel = modals.reject;
        $scope.submit = function() {
			modals.resolve( $scope.key );
        };
    }
);
