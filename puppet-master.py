#!/usr/bin/env python

import os
import json
import sys
import signal
import platform

# Terminal Emulator used to spawn the processes

terminal = "kitty"
terminal_run_path = "kitty"

if platform.system() == "Darwin":
    terminal_run_path = "/Applications/kitty.app/Contents/MacOS/kitty"

# Blockchain node configuration file name
server_configs = [
    "test_2.json"
]


server_config = server_configs[0]

def quit_handler(*args):
    os.system(f"pkill -i {terminal}")
    sys.exit()


# Compile classes
os.system("mvn clean install")

# Spawn blockchain nodes
with open(f"Configs/{server_config}") as f:
    data = json.load(f)
    processes = list()
    for key in data['nodes']:
        pid = os.fork()
        if pid == 0:
            key_dir = f"Keys/Node{key['id']}"
            try:
                os.makedirs(key_dir)
            except Exception as e:
                pass
            os.chdir(key_dir)
            os.system(f"openssl genrsa -out server.key")
            os.chdir(f"..")
            os.system(f"openssl rsa -pubout -in Node{key['id']}/server.key -out public{key['id']}.key")
            os.chdir(f"..")
            if not key['isClient']:
                os.system(f"{terminal_run_path} sh -c \"cd Service; mvn exec:java -Dexec.args='{key['id']} ../Configs/{server_config}' ; sleep 500\"")
            sys.exit()

signal.signal(signal.SIGINT, quit_handler)

while True:
    print("Type quit to quit")
    command = input(">> ")
    if command.strip() == "quit":
        quit_handler()
