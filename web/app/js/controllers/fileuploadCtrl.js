app.directive('demoFileModel', function ($parse) {
return {
    restrict: 'A', //the directive can be used as an attribute only
    link: function (scope, element, attrs) {
        var model = $parse(attrs.demoFileModel),
            modelSetter = model.assign;  
        element.bind('change', function () {
            scope.$apply(function () {
                modelSetter(scope, element[0].files[0]);
            });
        });
    }
}});

app.service('fileUploadService', function ($http, $q) {
	 
    this.uploadFileToUrl = function (file, uploadUrl) {
        var fileFormData = new FormData();
        fileFormData.append('file', file);

        var deffered = $q.defer();
        $http.post(uploadUrl, fileFormData, {
            transformRequest: angular.identity,
            headers: {'Content-Type': undefined, 'fileName': file.name}

        }).success(function (response) {
            deffered.resolve(response);

        }).error(function (response) {
            deffered.reject(response);
        });

        return deffered.promise;
    }
});

app.controller('FileUploadController', function ($scope, $http, fileUploadService) {
	 
    $scope.events = [];
    $scope.event = "";
    
	$scope.uploadFile = function () {
        var file = $scope.myFile;
        var uploadUrl = "/fileupload/" + 
        				$scope.event,
        				 //Url of webservice/api/server
            promise = fileUploadService.uploadFileToUrl(file, uploadUrl);

        promise.then(function (response) {
            $scope.serverResponse = response;
        }, function () {
            $scope.serverResponse = 'An error has occurred';
        })
    };
    
    // load dropdownlist
    $http.get('/map-api/v1/mappings?new=yes', 
			{}).
	then(
		function(response) {
			$scope.events = response.data;
		}, 
		function(response) {
			$scope.errorMessage = "There was an error in loading the mappings."
		}
	);

});