### BitTorrent P2P File Transfer - Project Read Me

#### Group 1 Members
- **Adam Eldredge:** 18047811
- **Jose Figueredo:** 57047240
- **Eric Mercier:** 11771839

### Contributions:

#### Adam
- **Startup Process:** Initializing peerProcess and establishing TCP connections to neighbors.
- **Message Handler:** Decoding messages, formatting, and parsing.
- **Protocol Logic:** Handling piece reception and transmission, random indexing, maintaining requested indices.
- **Debugging and Testing:** Identifying and resolving errors, testing on CISE machines, conducting the demo walkthrough and recording.

#### Jose
- **Peer Logger Implementation:** Creating subdirectories and handling logging operations.
- **Handshake and Bitfield Messages:** Connecting and managing these messages.
- **Functionality Implementation:** Initial bitfield setup, preferred neighbors, choke and unchoke logic, refactoring architecture.
- **Debugging and Fixing Errors:** Resolving bugs and issues.

#### Eric
- **Bitfield Initialization:** Setting bitfield size based on file and piece sizes.
- **Message Sending Functions:** Implementing sendHave, sendBitfield, sendRequest, sendPiece functionalities.
- **Debugging and Error Fixing:** Identifying and resolving various errors.

### Video Presentation:
[View Demo Video Here](https://uflorida-my.sharepoint.com/:v:/g/personal/adameldredge_ufl_edu/EUi6xbFsj9hJkXzir6rrbNMBa2qT4ZJUA2XTgVUXM0U5WA?e=fMgoDr)

### Achievements:
- Implemented a fully functioning BitTorrent P2P file transfer protocol.
- Complete implementation of message types including Handshake, Bitfield, Choke, Unchoke, Interested, Not Interested, Request, Piece, Optimistic Unchoking, Preferred Neighbors, and Logging.

### Challenges Faced:
- Encountered occasional socket errors on termination.
- Unable to initiate 9 connections to CISE machines, completing the project with 4 connections during the demo.

### How to Run the Project:

#### Transfer Files to Remote Machines
- **First-Time Setup:**
    - Connect via SSH to a remote machine: `ssh username@peerhostname`
    - Open an SFTP session: `sftp username@peerhostname`
    - Transfer project files using `put -r "local/file/pathtoproject" ~`
    - Untar the project using `tar -xvf archive_name.tar`

#### Running the Project:
- **Repeat for Each Peer:**
    - Connect via SSH to a remote machine: `ssh username@peerhostname`
    - SSH into a specific machine: `ssh specific_machine_name.cise.ufl.edu`
    - Navigate to the networking project directory
    - Compile using `javac peerProcess.java`
    - Run using `java peerProcess pid` (e.g., `java peerProcess 1001` for a specific host)
