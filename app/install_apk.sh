#/system/bin/sh
if ["$2" == ""]; then
   adb kill-server
   adb shell "am force-stop com.amazmod.service; busybox nohup sh /sdcard/install_apk.sh $1 OK &"
fi  
tag="AmazMod install_apk"
systype=$(getprop | grep display.id)
{
date
echo "System: $systype"
echo "PWD: $PWD"
echo "installing: $1"
adb shell pm install -r $1
echo "$1 installed"
sleep 3
adb shell am force-stop com.huami.watch.launcher
echo "launcher restarted"
sleep 3
adb shell dpm set-device-owner com.amazmod.service/.AdminReceiver
echo "device ownner set"
rm /sdcard/install_apk.sh
rm $1
echo "installation finished"
} | busybox tee /dev/tty | while read line; do
   log -p d -t "$tag" "$line"
done
adb kill-server
