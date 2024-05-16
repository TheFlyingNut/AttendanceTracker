const functions = require("firebase-functions");
const admin = require("firebase-admin");

admin.initializeApp();

exports.markAttendance = functions.database.ref("/connected_devices/{date}/{deviceId}")
  .onCreate(async (snapshot, context) => {
    const date = context.params.date;
    const devices = snapshot.val();
    const macAddresses = devices.map((device) => device.split(":")[1].trim());

    const userSnapshot = await admin.database().ref("/users").once("value");
    const users = userSnapshot.val();

    const promises = macAddresses.map(async (macAddress) => {
      for (const userId in users) {
        if (users[userId].macAddress === macAddress) {
          await admin.database().ref(`/attendance/${date}/${userId}`).set(true);
          break;
        }
      }
    });

    await Promise.all(promises);
    return null;
  });
