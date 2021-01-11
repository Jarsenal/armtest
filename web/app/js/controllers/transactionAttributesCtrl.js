app.controller(
    "transactionAttributesController",
    function( $scope, $http, modals ) {
        $scope.loading = false;
        $scope.currentevent = "";
        $scope.currentstart = "";
        $scope.currentfail = "";
        
        function mySearchTransactions(timespan, event, onlyfail,start, recordlimit,attribute){
                $scope.loading = true;
                $scope.currentevent = event;
                $scope.currentfail = onlyfail;
                $scope.currentstart = start;

                let config = {
                    headers : {'Content-Type': 'application/json'},
                    params : {'code':$scope.biibKey,
                            'span':timespan,
                            'event':(event!="")?event:null,
                            'start':start,
                            'recordlimit':recordlimit,
                            'attribute': attribute
                }
            };
                
                
            let url = $scope.apiHost + "/transactions/attributesearch";
           
            $http.get(url, config).
            then(
                function(response) {
                    $scope.loading = false;
                    $scope.transactions = response.data;

                    if($scope.transactions.length >= 100){
                        for(item in $scope.transactions){
                            $scope.currentstart = $scope.transactions[item].insertDateTime;
                        }
                    }
                    else{
                        $scope.currentstart = "";
                    }
                }, 
                function(response) {
                    $scope.loading = false;
                    alert("There was an error loading the events");
                }
            );
        }

        function getDateString(date, withtime){
            var day = date.getDate() + "";
            var month = (date.getMonth() + 1) + "";
            var year = date.getFullYear() + "";
            var hour = date.getHours() + "";
            var minutes = date.getMinutes() + "";
            var seconds = date.getSeconds() + "";
            
            day = checkZero(day);
            month = checkZero(month);
            year = checkZero(year);
            if(withtime){
                hour = checkZero(hour);
                minutes = checkZero(minutes);
                seconds = checkZero(seconds);
            }
            else {
                hour = "00";
                minutes = "00";
                seconds = "00";
            }
            
            function checkZero(data){
              if(data.length == 1){
                data = "0" + data;
              }
              return data;
            }
            return year + "-" + month + "-" + day + "T" + hour + ":" + minutes + ":" + seconds + ".000";
        }
        
        $scope.transactions = [];
        $scope.events = [];
        $scope.search = {
            transaction: "",
            event: "",
            status: "",
            start: getDateString(new Date(),false),
            end: ""
        }
        
        $scope.searchTransactions = function(timespan, event, failures, start, recordlimit,attribute) {
            mySearchTransactions(timespan, event, failures, start, recordlimit,attribute);
        };

        $scope.checkAll = function(selectValue){
            for(transaction in $scope.transactions){
                $scope.transactions[transaction].select = selectValue;
            }
        }

        $scope.reprocess = function(){

            let list = [];
            for(transaction in $scope.transactions){
                if($scope.transactions[transaction].select){
                    list.push($scope.transactions[transaction]);
                }
            }

            let promise = modals.open("reprocess",list)
            promise.then(
                function handleResolve( response ) {
                    console.log( "Done");
                    
                },
                function handleReject( error ) {
                    console.warn( "Done" );
                }
            );
        }

        

        $scope.details = function(transaction){
            var promise = modals.open("details",transaction);
            promise.then(
                function handleResolve( response ) {
                    console.log( "Done");
                    
                },
                function handleReject( error ) {
                    console.warn( "Done" );
                }
            );
        }
        
        $scope.timespan = "24";
        $scope.recordlimit = "25";

          // load dropdownlist
          let config2 = {params : {'code':$scope.biibKey}};
          $scope.eventlist = [];
          $scope.eventfilter = "";
          $http.get($scope.apiHost + '/configs/events/',config2).
          then(
              function(response) {
                  for(item in response.data){
                      $scope.eventlist.push({
                          key: response.data[item].name,
                          value: response.data[item].name
                      });
                  }
              }, 
              function(response) {
                  alert("There was an error loading the events");
              }
          );
    }
);




// app.controller(
//     "getdetails",
//     function( $scope, $http, modals ) {
//         $scope.transaction =  modals.params();
//         $scope.errorMessage = null;
//         $scope.cancel = modals.reject;
//         $scope.submit = function() {
//             modals.resolve( {} );
//         };

//         $scope.selectTransaction = function(index){
//             $scope.transaction = $scope.transactions[index];
//             $scope.payload = "";
//         }

//         $scope.downloadTransaction = function(transaction){
            
            
//             let config = {
//                 headers : {'Content-Type': 'application/json'},
//                 params : {  'code':$scope.biibKey,
//                             'key': transaction.blobKey
//                         },
//                 transformResponse: [function(data) {return data;}]
//             };
                
                
//             let url = $scope.apiHost + "/transactions/payload";
           
//             $http.get(url, config).
//             then(
//                 function(response) {
//                     $scope.payload = response.data;
//                 }, 
//                 function(response) {
//                     alert(response.data);
//                 }
//             );
//         }

//         let config = {
//             headers : {'Content-Type': 'application/json'},
//             params : {  'code':$scope.biibKey,
//                         'transaction': $scope.transaction.transactionId,
//                         'event': $scope.transaction.event}
//         };
            
            
//         let url = $scope.apiHost + "/transactions/transaction";
       
//         $http.get(url, config).
//         then(
//             function(response) {
//                 $scope.loading = false;
//                 $scope.transactions = response.data;
//                 $scope.selectTransaction(0);
//             }, 
//             function(response) {
//                 $scope.loading = false;
//                 alert("There was an error loading the events");
//             }
//         );
//     }
// );




// app.controller(
//     "reprocessCtrl",
//     function( $scope, $http, modals ) {
//         $scope.transactions =  modals.params();
//         $scope.errorMessage = null;
//         $scope.cancel = modals.reject;
//         var transactlist = [];
//         $scope.submit = function() {
        
//             let url = $scope.apiHost + "/transactions/reprocess";
//             let config = {
//                 headers : {'Content-Type': 'application/json'},
//                 params : {  'code':$scope.biibKey}
//             };
            

//             $http.post(url, transactlist, config).
//             then(
//                 function(response) {
//                     alert(response.data);
//                     modals.resolve( {} );
//                 }, 
//                 function(response) {
//                     $scope.loading = false;
//                     alert("There was an error loading the events");
//                 }
//             );
//         };

//         let transactionIdList = [];

//         for(transaction in $scope.transactions){
//             if(transactionIdList.indexOf($scope.transactions[transaction].transactionId) < 0)
//                 transactionIdList.push($scope.transactions[transaction].transactionId)
//         }

//         let url = $scope.apiHost + "/transactions/findsource";
//         let config = {
//             headers : {'Content-Type': 'application/json'},
//             params : {  'code':$scope.biibKey}
//         };
        

//         $http.post(url, transactionIdList, config).
//         then(
//             function(response) {
               
//                 let eventlist = {};
//                 let list = response.data;
//                 transactlist = [];

//                 for(transaction in list){
//                     if(eventlist[list[transaction].event]){
//                         eventlist[list[transaction].event] += 1;
//                     }
//                     else {
//                         eventlist[list[transaction].event] = 1;
//                     }
//                     transactlist.push({
//                         event:list[transaction].event,
//                         transaction:list[transaction].transactionId
//                     })
//                 }

//                 $scope.events = []
//                 for(event in eventlist){
//                     $scope.events.push({
//                         name: event,
//                         count: eventlist[event]
//                     })
//                 }
//             }, 
//             function(response) {
//                 $scope.loading = false;
//                 alert("There was an error loading the events");
//             }
//         );

//     }
// );
