#!/usr/bin/env python

import os
import json
import sys
import signal


# Terminal Emulator used to spawn the processes
terminal = "kitty"
terminal_mac = "/Applications/kitty.app/Contents/MacOS/kitty"

# Blockchain node configuration file name
server_configs = [
    "regular_config.json",
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
    print(len(data))
    for key in data:
        pid = os.fork()
        if pid == 0:
            key_dir = f"Keys/Node{key['id']}"
            os.chdir(key_dir)
            os.system(f"openssl genrsa -out server.key")
            os.chdir(f"..")
            os.system(f"openssl rsa -pubout -in Node{key['id']}/server.key -out public{key['id']}.key")
            os.chdir(f"..")
            if not key['isClient']:
                os.system(f"{terminal_mac} sh -c \"cd Service; mvn exec:java -Dexec.args='{key['id']} {server_config} {key_dir}' ; sleep 500\"")
            sys.exit()

signal.signal(signal.SIGINT, quit_handler)

while True:
    print("Type quit to quit")
    command = input(">> ")
    if command.strip() == "quit":
        quit_handler()
