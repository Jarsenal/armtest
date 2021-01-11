var app = angular.module('app', [ 'ngRoute','ngAnimate']) //,'ui.tree' ,'ui.bootstrap'
.config(function ($routeProvider) {

    $routeProvider.when("/mapping", {
        templateUrl: "views/mapping.html",
        controller: "mappingCtrl"
//    }).when("/query", {
//        templateUrl: "views/query.html",
//        controller: "queryController"
//    }).when("/fileupload", {
//    	templateUrl: "views/fileupload.html",
//    	controller: "FileUploadController"
     }).when("/routing", {
    	templateUrl: "views/routing.html",
    	controller: "routingController"
//    }).when("/xreffiletoevent", {
//    	templateUrl: "views/xreffiletoevent.html",
//    	controller: "xreffiletoeventController"
//    }).when("/schedulestoevents", {
//    	templateUrl: "views/schedulestoevents.html",
//    	controller: "schedulestoeventsController"
    }).when("/event", {
    	templateUrl: "views/events.html",
    	controller: "eventController"
    }).when("/transactions", {
    	templateUrl: "views/transactions.html",
    	controller: "transactionsController"
    }).when("/mqtoevent", {
    	templateUrl: "views/mqtoevent.html",
        controller: "mqtoeventController"
    }).when("/attributesearch", {
        templateUrl: "views/transactionattributes.html",
        controller: "transactionAttributesController"
//    }).when("/batches", {
//    	templateUrl: "views/batches.html",
//    	controller: "batchesController"
//    }).when("/reprocess", {
//    	templateUrl: "views/reprocess.html",
//    	controller: "reprocessController"
//    }).when("/holds", {
//    	templateUrl: "views/holds.html",
//    	controller: "holdsController"
    }).when("/help", {
    	templateUrl: "views/help.html"
    })
    .otherwise({ redirectTo: "/help" });
});

