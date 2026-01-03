from app.modules.game_logger.monitoring import ModelMonitoringService
from app.modules.game_logger.service import GameLoggerService

def main():
    monitor = ModelMonitoringService()
    monitor.log_from_database(GameLoggerService())
    monitor.finish()
    print("Monitoring run completed")

if __name__ == "__main__":
    main()
