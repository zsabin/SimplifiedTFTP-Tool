# SimplifiedTFTP-Tool
A client-server tool that implements a simplified version of the TFTP Protocol [(IETF RFC 1350)](http://www.ietf.org/rfc/rfc1350.txt). This tool differs from the protocol specification in that it only allows clients to send files and it supports only binary (octet) transmission. It also supports a few functionalities beyond those specified in the protocol: it allows the user to choose between sending IPv4 or IPv6 UDP datagrams and between two data transmission protocols (TCP-style sliding windows or sequential ACKs), and it supports a command-line flag, which, when set, causes the client to simulate dropping 1 percent of the packets sent.

I developed this tool for my CSC 445 Computer Networks course in Spring 2016.
