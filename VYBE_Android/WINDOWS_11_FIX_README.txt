VYBE Windows 11 fixed builder

This update prevents benign Java stderr messages from aborting the PowerShell build.
A build-log.txt file is created immediately, including when setup fails.
The builder retries online if the offline cache is incomplete.

Extract ZIP, open VYBE_Android, run BUILD_APK.bat.
If it fails, upload VYBE_Android\build-log.txt.
The APK has not been compiled or tested here.
