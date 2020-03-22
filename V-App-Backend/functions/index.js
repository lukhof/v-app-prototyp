const functions = require('firebase-functions');
const admin = require('firebase-admin');
const messaging = require('firebase-messaging')

admin.initializeApp(functions.config().firebase);

//Some config vars, change in production
const state = 'debug';
const main_collection = 'test';
const time_offset = 15; //In mins
const geo_offset = 1000; //in m

/**
 * @name drawCircle
 * @description Calculates the circle
 * @param {array}
 * @returns {GeoPoint, number}
 * @author Pascal
 */
function drawCircle(array) {

    var lat = [];
    var lon = [];

    array.map(function(obj) {
        lat.push(obj.geo.latitude);
        lon.push(obj.geo.longitude);
    });

    var lat_m = (Math.max(...lat) + Math.min(...lat)) / 2;
    var lon_m = (Math.max(...lon) + Math.min(...lon)) / 2;

    var range = Math.max(...[Math.max(...lat) - lat_m, Math.max(...lon) - lon_m]);

    return {
            location: new admin.firestore.GeoPoint(lat_m, lon_m),
            range: range
        };
}

/**
 * @name addInfected
 * @description Adds a infected person to the database and calculates the circle
 * @author Pascal
 */
exports.addInfected = functions.firestore
    .document(main_collection + '/{id}')
    .onCreate((snap, context) => {

        const data = snap.data();
        const id = snap.id;

        if (data.hasOwnProperty("values")) {
            const values = data.values;

            const points = admin.firestore().collection(main_collection).doc(id);
            var circle = drawCircle(values);

            var info = {
                data: {
                    unit: state,
                    lat: String(circle.location.latitude),
                    lon: String(circle.location.longitude),
                    range: String(circle.range),
                    id: id
                },
                topic: 'new_infection'
            };

            admin.messaging().send(info);
            return admin.firestore().doc(main_collection + '/' + id).update({middle: circle.location, range: circle.range, time: admin.firestore.FieldValue.serverTimestamp()});
        } else {
            return null;
        }
    });


/**
 * @name isInfected
 * @description Checks if a person is infected by compairing the datapoints
 * @returns {result: bool}
 * @author Pascal
 */
exports.isInfected = functions.https.onRequest((req, res) => {

    if(!req.body.data.hasOwnProperty("id")) {
        res.send( {
            data: {
                result: false,
                error: true,
                message: 'id property not set'
            }
        });
        return;
    }

    if(!req.body.data.hasOwnProperty("values")) {
        res.send( {
            data: {
                result: false,
                error: true, 
                message: 'values property not set'
            }
        });
        return;
    }

    var circle = drawCircle(req.body.data.values);
    var id = req.body.data.id;
    var data = req.body.data.values;
    admin.firestore().collection(main_collection).doc(id).get()
    .then(snapshot => {
        if(!snapshot.exists) {
            res.send( {
                data: {
                    error: true,
                    result: false,
                    message: "Does not exists"
                }
            });
            return false;
        } else {
        for (var i = 0; i < snapshot.data().values.length; i++) {
            var obj = snapshot.data().values[i];
    
            if (obj.geo.latitude > (circle.location.latitude - circle.range) 
                && obj.geo.latitude < (circle.location.latitude + circle.range) 
                && obj.geo.longitude > (circle.location.longitude - circle.range) 
                && obj.geo.longitude < (circle.location.longitude + circle.range)
                ){
                for (var ii = 0; ii < data.length; ii++) {
                    var obj2 = data[ii];
                    if (obj.geo.latitude > (obj2.geo.latitude - geo_offset) 
                        && obj.geo.latitude < (obj2.geo.latitude + geo_offset)
                        && obj.geo.longitude > (obj2.geo.longitude - geo_offset) 
                        && obj.geo.longitude < (obj2.geo.longitude + geo_offset)
    
                        && obj.time.toMillis() > (obj2.time - time_offset * 60) 
                        && obj.time.toMillis() < (obj2.time + time_offset * 60)) {
                        res.send( {
                            data: {
                                result: true,
                                error: false
                            }
                        });
                        console.log("Found Data");
                        return true;
                    }
                }
            }
        }
        res.send({
            data: {
                result: false,
                error: false
            }
        });
        return false;
    }
    }).catch(err => {
        console.log(err);
        res.send( {
            data: {
                result: false,
                error: true,
                message: "Es ist irgendwas kaputt :("
            }
        });
    });
});