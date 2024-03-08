#!/usr/bin/env python

from random import choice, shuffle
from string import ascii_lowercase, digits
import os
import json
import sys
import signal
import platform
import time
import subprocess
import re

# Tests
tests = [
    "regular_config.json",
    "test_1.json",
    "test_2.json"
]

# Terminal Emulator used to spawn the processes
terminal = "kitty"
terminal_run_path = terminal

if platform.system() == "Darwin":
    terminal_run_path = "/Applications/kitty.app/Contents/MacOS/kitty"

def quit_servers():
    os.system(f"pkill -i {terminal}")
    os.system(f"pkill -i {terminal}")
    os.system("cd Service; rm *.txt")
    os.system("cd Client; rm *.txt")

def spawn_servers(data, server_config):
    # Spawn blockchain nodes
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
                os.system(f"{terminal_run_path} sh -c \"cd Service; mvn exec:java -Dexec.args='{key['id']} ../Configs/{server_config}' --log-file ./Node{key['id']}_result.txt ; sleep 500\"")
            sys.exit()

def run_test(config):
    with open(f"Configs/{config}") as f:
        data = json.load(f)
        clients = list(filter(lambda node: node['isClient'], data['nodes']))
        servers = list(filter(lambda node: not node['isClient'], data['nodes']))

        test_instances = len(data['tests'])

        # First generate a random list of ledgers
        chars = ascii_lowercase + digits
        expected_ledgers = [(''.join(choice(chars) for _ in range(4))) for _ in range(test_instances * len(clients))]
        input_ledgers = [expected_ledgers[i::len(clients)] for i in range(len(clients))]

        print(f"Ledgers to append: {input_ledgers}")

        # Spawn servers
        print("Starting servers...")
        spawn_servers(data, config)

        print("Starting clients...")
        time.sleep(6)

        # Spawn client
        processes = []
        results = []
        input_ledgers_index = 0

        for client in clients:
            # The command is passed as a list of tokens

            cmd = f"cd Client; mvn exec:java -Dexec.args='{client['id']} ../Configs/{config} true {','.join(input_ledgers[input_ledgers_index])}' --log-file ./Node{client['id']}_result.txt"

            print(f"Launching: {cmd}")

            proc = subprocess.Popen(cmd, shell=True, stdout=subprocess.PIPE, stderr=subprocess.PIPE)
            processes.append(proc)
            input_ledgers_index = input_ledgers_index + 1

        print("Waiting for tests...")
        # Wait for each process to complete and capture its output and return code
        input_ledgers_index = 0
        for proc in processes:
            stdout, stderr = proc.communicate()  # This waits for the process to finish and captures output
            return_code = proc.returncode
            if (return_code != 0):
                print(f"[x] Test failed at node {input_ledgers_index}")
                with open(f"./Client/Node{clients[input_ledgers_index]['id']}_result.txt", "r") as o:
                    print(''.join(o.readlines()))
                quit_servers()
                return
            input_ledgers_index = input_ledgers_index + 1

        time.sleep(1)

        # Check server logs
        blockchains = []
        for key in data['nodes']:
            if not key['isClient']:
                with open(f"./Service/Node{key['id']}_result.txt", "r") as o:
                    pattern = r"Current Ledger: ([a-z0-9]+)\n"
                    matches = re.findall(pattern, ''.join(o.readlines()))
                    if matches:
                        blockchains.append(matches[-1])

        quit_servers()

        if len(blockchains) == 0:
            print(f"[x] Test failed: Could not parse servers blockchains (Current Ledger)")
            return

        print(f"Servers Nodes Blockchains: {blockchains}")
        print("Validating blockchains...")

        # Check if all the blockchains match
        index = 0
        for chain in blockchains:
            if chain != blockchains[0]:
                print(f"[x] Test failed: Blockchains of node {servers[0]['id']} and node {servers[index]['id']} differ! ({blockchains[0]} != {chain})")
                return
            index = index + 1

        # Split blockchain in blocks with the initial size (4)
        blocks = [blockchains[0][i:i+4] for i in range(0, len(blockchains[0]), 4)]

        if len(blocks) < len(expected_ledgers):
            print(f"[x] Test failed: Blockchain is incomplete! Missing {len(expected_ledgers) - len(blocks)} ledgers")
            return

        # Finally check if all the block are right
        invalid_blocks = blocks.copy()
        for block in blocks:
            if block in expected_ledgers:
                invalid_blocks.remove(block)

        if len(invalid_blocks) > 0:
            print(f"[x] Test failed: Blockchain is invalid! Invalid blocks: {invalid_blocks}")
            return

        print("==> Test passed!")

def main():
    # Compile classes
    os.system("mvn clean install")

    for test in tests:
        run_test(test)

if __name__ == "__main__":
    main()
