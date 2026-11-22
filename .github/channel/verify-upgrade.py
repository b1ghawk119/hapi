"""Upgrade between two public releases through HAPI and Android's installer."""
import os
import re
import subprocess
import time
import xml.etree.ElementTree as ET
from pathlib import Path

PKG = 'run.hapi.companion.b1ghawk119'
evidence = Path('update-evidence')
evidence.mkdir(exist_ok=True)


def adb(*args):
    return subprocess.check_output(['adb', *args], text=True, timeout=30)


def shot(name):
    (evidence / f'{name}.png').write_bytes(subprocess.check_output(['adb', 'exec-out', 'screencap', '-p']))


def state():
    dump = adb('shell', 'dumpsys', 'package', PKG)
    return {k: re.search(r'\b' + k + r'=([^\n]+)', dump)[1].strip().split(' minSdk=')[0]
            for k in ['versionCode', 'firstInstallTime']}


def wait_for(labels, timeout=120):
    deadline = time.monotonic() + timeout
    while time.monotonic() < deadline:
        screen = adb('exec-out', 'uiautomator', 'dump', '/dev/tty')
        if '</hierarchy>' not in screen:
            continue
        root = ET.fromstring(screen[screen.index('<?xml'):screen.index('</hierarchy>') + 12])
        for node in root.iter('node'):
            if node.get('text', '').strip().casefold() in labels or node.get('resource-id') in labels:
                return node
        time.sleep(1)
    raise RuntimeError(f'Could not find {labels}')


def tap(node):
    x1, y1, x2, y2 = map(int, re.findall(r'\d+', node.attrib['bounds']))
    adb('shell', 'input', 'tap', str((x1+x2)//2), str((y1+y2)//2))


try:
    adb('install', str(Path(os.environ['RUNNER_TEMP']) / 'previous/HAPI.apk'))
    before = state()
    assert before['versionCode'] == os.environ['FROM'], before
    adb('shell', 'am', 'start', '-W', '-n', f'{PKG}/app.hapi.companion.MainActivity')
    action = wait_for({'download and install'})
    shot('01-update-prompt')
    tap(action)
    switch = wait_for({'allow from this source', 'android:id/switch_widget', 'com.android.settings:id/switch_widget'})
    shot('02-install-permission')
    tap(switch)
    adb('shell', 'input', 'keyevent', '4')
    confirm = wait_for({'update', 'install'})
    shot('03-system-installer')
    tap(confirm)
    for i in range(60):
        after = state()
        if after['versionCode'] == os.environ['TO']:
            break
        time.sleep(1)
    assert after == {**before, 'versionCode': os.environ['TO']}, (before, after)
    adb('shell', 'am', 'start', '-W', '-n', f'{PKG}/app.hapi.companion.MainActivity')
    assert 'HAPI update available' not in adb('exec-out', 'uiautomator', 'dump', '/dev/tty')
    shot('04-updated-app')
    print(f'In-place upgrade: {before} -> {after}')
finally:
    (evidence / 'last-screen.xml').write_text(adb('exec-out', 'uiautomator', 'dump', '/dev/tty'))
    shot('last-screen')
    (evidence / 'logcat.txt').write_text(adb('logcat', '-d', '-t', '2000'))
