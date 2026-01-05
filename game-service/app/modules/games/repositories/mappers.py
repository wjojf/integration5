"""
Mappers between domain models and ORM models.
"""
from typing import Optional

from ..domain.models import GameSession
from .models import GameSessionModel


class GameSessionMapper:
    """Mapper for GameSession domain model."""

    @staticmethod
    def to_orm(domain: GameSession) -> GameSessionModel:
        """Convert domain model to ORM model."""
        return GameSessionModel(
            session_id=domain.session_id,
            game_id=domain.game_id,
            game_type=domain.game_type,
            lobby_id=domain.lobby_id,
            player_ids=domain.player_ids,
            current_player_id=domain.current_player_id,
            status=domain.status,
            game_state=domain.game_state,
            started_at=domain.started_at,
            ended_at=domain.ended_at,
            winner_id=domain.winner_id,
            total_moves=domain.total_moves,
            game_metadata=domain.game_metadata
        )

    @staticmethod
    def to_domain(orm: GameSessionModel) -> GameSession:
        """Convert ORM model to domain model."""
        return GameSession(
            session_id=orm.session_id,
            game_id=orm.game_id,
            game_type=orm.game_type,
            lobby_id=orm.lobby_id,
            player_ids=orm.player_ids,
            current_player_id=orm.current_player_id,
            status=orm.status,
            game_state=orm.game_state,
            started_at=orm.started_at,
            ended_at=orm.ended_at,
            winner_id=orm.winner_id,
            total_moves=orm.total_moves,
            game_metadata=orm.game_metadata or {}
        )

    @staticmethod
    def update_orm_from_domain(orm: GameSessionModel, domain: GameSession) -> None:
        """Update ORM model from domain model."""
        orm.game_id = domain.game_id
        orm.game_type = domain.game_type
        orm.lobby_id = domain.lobby_id
        orm.player_ids = domain.player_ids
        orm.current_player_id = domain.current_player_id
        orm.status = domain.status
        orm.game_state = domain.game_state
        orm.ended_at = domain.ended_at
        orm.winner_id = domain.winner_id
        orm.total_moves = domain.total_moves
        orm.game_metadata = domain.game_metadata















