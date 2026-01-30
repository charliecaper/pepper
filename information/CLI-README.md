# EvenHub CLI
2
3Command-line interface for EvenHub development and app management.
4
5## Quick Start
6
7**For development mode with the Even app, the `qr` command is the only command you need in this phase.**
8
9## Commands
10
11### `qr` - Generate QR Code
12
13Generate a QR code for your development server URL. This is the primary command for using dev mode with the Even app.
14
15**Basic usage:**
16```bash
17evenhub qr
18```
19
20The command will automatically detect your local IP address and prompt you for the port and path. On subsequent runs, it will remember your previous settings.
21
22**Options:**
23- `-u, --url <url>` - Provide a full URL directly (overrides other options)
24- `-i, --ip <ip>` - Specify the IP address or hostname
25- `-p, --port [port]` - Specify the port (leave empty for no port)
26- `--path <path>` - Specify the URL path
27- `--https` - Use HTTPS instead of HTTP
28- `--http` - Use HTTP instead of HTTPS
29- `-e, --external` - Open QR code in an external program instead of terminal
30- `-s, --scale <scale>` - Scale factor for external QR code (default: 4)
31- `--clear` - Clear cached scheme, IP, port, and path settings
32
33**Examples:**
34```bash
35# Generate QR code with auto-detected IP
36evenhub qr
37
38# Generate QR code for a specific URL
39evenhub qr --url http://192.168.1.100:3000
40
41# Generate QR code with specific IP and port
42evenhub qr --ip 192.168.1.100 --port 3000
43
44# Open QR code externally
45evenhub qr --external
46```
47
48### `init` - Initialize Project
49
50Initialize a new project with a basic `app.json` configuration file.
51
52**Usage:**
53```bash
54evenhub init [options]
55```
56
57**Options:**
58- `-d, --directory <directory>` - Directory to create the project in (default: `./`)
59- `-o, --output <output>` - Output file path (takes precedence over `--directory`, default: `./app.json`)
60
61**Example:**
62```bash
63evenhub init
64evenhub init --directory ./my-app
65evenhub init --output ./config/app.json
66```
67
68### `login` - Login to EvenHub
69
70Log in using your Even Realities account (same one used in app). Credentials are saved locally for future use.
71
72**Usage:**
73```bash
74evenhub login [options]
75```
76
77**Options:**
78- `-e, --email <email>` - Your email address
79
80**Example:**
81```bash
82evenhub login
83evenhub login --email user@example.com
84```
85
86### `pack` - Pack Project
87
88Pack your project into an `.ehpk` file ready for app creation/submit.
89
90**Usage:**
91```bash
92evenhub pack <json> <project> [options]
93```
94
95**Arguments:**
96- `<json>` - Path to your `app.json` metadata file
97- `<project>` - Path to your built project folder (e.g., `dist`, `build`)
98
99**Options:**
100- `-o, --output <output>` - Output file name (default: `out.ehpk`)
101- `--no-ignore` - Include hidden files (those starting with `.`)
102- `-c, --check` - Check if the package ID is available
103
104**Example:**
105```bash
106evenhub pack app.json ./dist
107evenhub pack app.json ./build --output my-app.ehpk
108evenhub pack app.json ./dist --check
109```
110