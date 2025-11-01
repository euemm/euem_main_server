module.exports = {
	apps: [
		{
			name: "euem-main-server",
			cwd: "/home/deploy/euem_main_server",
			script: "java",
			args: [
				"-Xms256m",
				"-Xmx512m",
				"-XX:+UseG1GC",
				"-jar",
				"build/libs/euem-main-server-0.0.1-SNAPSHOT.jar"
			],
			env: {
				SPRING_PROFILES_ACTIVE: "prod"
			},
			autorestart: true,
			instances: 1,
			max_memory_restart: "600M",
			pid_file: "pm2/pids/euem-main-server.pid",
			out_file: "pm2/logs/euem-main-server.out.log",
			error_file: "pm2/logs/euem-main-server.error.log"
		}
	]
};

