Group 1
Adam Eldredge: 18047811
Jose Figueredo: 57047240
Eric Mercier: 11771839

Adam
Startup process
peerProcess setup, making TCP connections to neighbors
Message Handler
Decode message and helper functions
Message formatting and parsing
Protocol logic regarding receiving and sending pieces
Random indexing and maintaining requested indices
Debugging and fixing errors
Testing on CISE machines and Demo walkthrough and recording


Jose
Peer Logger Implementation 
Create subdirectories
Connect to handshake messages
Connect to bitfield messages
The rest of peer Logger

Initial bitField setup
prefferNeighbors and OptUnchocked Interval functionality
Choke and Unchoke (downloading rate calculations)
Refactoring architecture
Debugging and fixing errors 

Eric
Initialize the size of the bitfield (based on file size and piece size from config)
Message send functions (155-185) 
sendHave
sendBitfield
sendRequest
sendPiece
Debugging and fixing errors 

Video link: 
https://uflorida-my.sharepoint.com/:v:/g/personal/adameldredge_ufl_edu/EUi6xbFsj9hJkXzir6rrbNMBa2qT4ZJUA2XTgVUXM0U5WA?e=fMgoDr

What we were able to do:
A fully functioning BitTorrent P2P file transfer protocol that transfers files between hosts.
Fully implemented message types
Handshake
Bitfield
Choke
Unchoke
Interested
Not interested 
Request
Piece 
Optimistic Unchoking
Preferred neighbors 
Connection termination (*see below)
Logging

What we were unable to do:
Occasional socket errors on termination
Extra note: We were unable to start 9 connections to the CISE machines - so we completed it with 4 in the demo, like mentioned on slack. We tried to do it through VSCode but were unable to get it to work through the integrated terminals.

How to run our project

Transfer files to remote machines
***Do once***
Connect to a remote machine by running ssh username@peerhostname (ej.mercier@storm.cise.ufl.edu)
Enter your password 
Open a new terminal and type  sftp username@peerhostname, (sftp ej.mercier@storm.cise.ufl.edu)
Enter your password
put -r "local/file/pathtoproject" ~
Run tar -xvf archive_name.tar to untar the project

Run project
***Repeat steps for each peer participating in the file share***
Connect to a remote machine by running ssh username@peerhostname (ej.mercier@storm.cise.ufl.edu)
Enter your password 
Ssh into specific machine (ex: ssh lin114-08.cise.ufl.edu)
Cd into the networking project directory
Compile using javac peerProcess.java
Run using java peerProcess pid (java peerProcess 1001) for the specific host you are running 

    
