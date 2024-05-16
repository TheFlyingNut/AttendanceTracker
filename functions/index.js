const functions = require("firebase-functions");
const admin = require("firebase-admin");

admin.initializeApp();

exports.markAttendance = functions.database
    .ref("/connected_devices/{date}/{pushId}")
    .onWrite((change, context) => {
        let devicesData = change.after.val();
        if (!devicesData) {
            console.log("No devices data found");
            return null;
        }

        // If devicesData is a string, convert it to an array
        if (typeof devicesData === 'string') {
            devicesData = [devicesData];
        }

        if (!Array.isArray(devicesData)) {
            console.log("Invalid devices data:", devicesData);
            return null;
        }

        // Clean up the device data
        const cleanedDevices = devicesData.map(device => {
            const parts = device.trim().split(': ');
            return parts.length > 1 ? parts[1].trim() : device.trim();
        });

        console.log("Cleaned devices list:", cleanedDevices);

        const usersRef = admin.database().ref("users");

        return usersRef.once("value").then((snapshot) => {
            const users = snapshot.val();
            if (!users) {
                console.log("No users found");
                return null;
            }

            const presentUsers = Object.entries(users).filter(([userId, userData]) =>
                cleanedDevices.includes(userData.macAddress)
            );

            const attendanceRef = admin.database().ref("attendance").child(context.params.date);

            const updates = {};
            presentUsers.forEach(([userId]) => {
                updates[userId] = true;
            });

            return attendanceRef.update(updates).then(() => {
                console.log("Attendance updated successfully for date:", context.params.date);
            }).catch((error) => {
                console.error("Error updating attendance:", error);
            });
        });
    });
