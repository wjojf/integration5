package com.banditgames.platform.shared.messaging;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ configuration for async messaging between platform-backend and game-service.
 * Configures exchanges, queues, bindings, and message converters.
 */
@Configuration
public class RabbitMQConfig {
    
    @Value("${game.events.exchange.name}")
    private String exchangeName;
    
    @Value("${game.events.queues.move-requests}")
    private String moveRequestsQueue;
    
    @Value("${game.events.queues.move-responses}")
    private String moveResponsesQueue;
    
    @Value("${game.events.queues.state-updates}")
    private String stateUpdatesQueue;
    
    @Value("${game.events.queues.achievements}")
    private String achievementsQueue;
    
    @Value("${game.events.queues.session-start-requested:game.session.start.requested}")
    private String sessionStartRequestedQueue;
    
    @Value("${game.events.queues.session-started}")
    private String sessionStartedQueue;
    
    @Value("${game.events.queues.session-ended}")
    private String sessionEndedQueue;
    
    // Separate queues for each consumer of session ended events
    // Both bound to same routing key = each receives a copy (no competing consumers)
    @Value("${game.events.queues.session-ended-lobby:game.session.ended.lobby}")
    private String sessionEndedLobbyQueue;
    
    @Value("${game.events.queues.session-ended-achievements:game.session.ended.achievements}")
    private String sessionEndedAchievementsQueue;
    
    @Value("${game.events.queues.move-applied:game.move.applied}")
    private String moveAppliedQueue;
    
    @Value("${game.events.dead-letter.exchange:game_events_dlx}")
    private String dlxName;
    
    @Value("${game.events.dead-letter.queue:game_events_dlq}")
    private String dlqName;
    
    /**
     * Topic exchange for game events.
     */
    @Bean
    public TopicExchange gameEventsExchange() {
        return ExchangeBuilder
            .topicExchange(exchangeName)
            .durable(true)
            .build();
    }
    
    /**
     * Dead Letter Exchange for failed messages.
     */
    @Bean
    public DirectExchange deadLetterExchange() {
        return ExchangeBuilder
            .directExchange(dlxName)
            .durable(true)
            .build();
    }
    
    /**
     * Dead Letter Queue.
     */
    @Bean
    public Queue deadLetterQueue() {
        return QueueBuilder
            .durable(dlqName)
            .build();
    }
    
    @Bean
    public Binding deadLetterBinding() {
        return BindingBuilder
            .bind(deadLetterQueue())
            .to(deadLetterExchange())
            .with(dlqName);
    }
    
    /**
     * Queue for move requests sent to game-service.
     */
    @Bean
    public Queue moveRequestsQueue() {
        return QueueBuilder
            .durable(moveRequestsQueue)
            .withArgument("x-dead-letter-exchange", dlxName)
            .withArgument("x-dead-letter-routing-key", dlqName)
            .build();
    }
    
    /**
     * Queue for move responses from game-service.
     */
    @Bean
    public Queue moveResponsesQueue() {
        return QueueBuilder
            .durable(moveResponsesQueue)
            .withArgument("x-dead-letter-exchange", dlxName)
            .withArgument("x-dead-letter-routing-key", dlqName)
            .build();
    }
    
    /**
     * Queue for game state updates from game-service.
     */
    @Bean
    public Queue stateUpdatesQueue() {
        return QueueBuilder
            .durable(stateUpdatesQueue)
            .withArgument("x-dead-letter-exchange", dlxName)
            .withArgument("x-dead-letter-routing-key", dlqName)
            .build();
    }
    
    /**
     * Queue for achievement unlock events.
     */
    @Bean
    public Queue achievementsQueue() {
        return QueueBuilder
            .durable(achievementsQueue)
            .withArgument("x-dead-letter-exchange", dlxName)
            .withArgument("x-dead-letter-routing-key", dlqName)
            .build();
    }
    
    /**
     * Queue for game session start requested events.
     * Published by platform-backend, consumed by game-service.
     */
    @Bean
    public Queue sessionStartRequestedQueue() {
        return QueueBuilder
            .durable(sessionStartRequestedQueue)
            .withArgument("x-dead-letter-exchange", dlxName)
            .withArgument("x-dead-letter-routing-key", dlqName)
            .build();
    }
    
    /**
     * Queue for game session started events.
     * Published by game-service, consumed by platform-backend.
     */
    @Bean
    public Queue sessionStartedQueue() {
        return QueueBuilder
            .durable(sessionStartedQueue)
            .withArgument("x-dead-letter-exchange", dlxName)
            .withArgument("x-dead-letter-routing-key", dlqName)
            .build();
    }
    
    /**
     * Queue for game session ended events (legacy - kept for backwards compatibility).
     */
    @Bean
    public Queue sessionEndedQueue() {
        return QueueBuilder
            .durable(sessionEndedQueue)
            .withArgument("x-dead-letter-exchange", dlxName)
            .withArgument("x-dead-letter-routing-key", dlqName)
            .build();
    }
    
    // ============================================
    // Session Ended Event Distribution
    // Multiple queues bound to same routing key = each receives a copy
    // This achieves fanout behavior without E2E binding complexity
    // ============================================
    
    /**
     * Queue for lobby cleanup on session ended.
     * Consumed by GameSessionEndedConsumer.
     */
    @Bean
    public Queue sessionEndedLobbyQueue() {
        return QueueBuilder
            .durable(sessionEndedLobbyQueue)
            .withArgument("x-dead-letter-exchange", dlxName)
            .withArgument("x-dead-letter-routing-key", dlqName)
            .build();
    }
    
    /**
     * Queue for achievements evaluation on session ended.
     * Consumed by GameEventConsumer (achievements).
     */
    @Bean
    public Queue sessionEndedAchievementsQueue() {
        return QueueBuilder
            .durable(sessionEndedAchievementsQueue)
            .withArgument("x-dead-letter-exchange", dlxName)
            .withArgument("x-dead-letter-routing-key", dlqName)
            .build();
    }
    
    /**
     * Bind lobby queue directly to topic exchange with session ended routing key.
     * When game-service publishes "game.session.ended", this queue receives a copy.
     */
    @Bean
    public Binding sessionEndedLobbyBinding() {
        return BindingBuilder
            .bind(sessionEndedLobbyQueue())
            .to(gameEventsExchange())
            .with("game.session.ended");
    }
    
    /**
     * Bind achievements queue directly to topic exchange with session ended routing key.
     * When game-service publishes "game.session.ended", this queue receives a copy.
     */
    @Bean
    public Binding sessionEndedAchievementsBinding() {
        return BindingBuilder
            .bind(sessionEndedAchievementsQueue())
            .to(gameEventsExchange())
            .with("game.session.ended");
    }
    
    @Bean
    public Binding moveRequestsBinding() {
        return BindingBuilder
            .bind(moveRequestsQueue())
            .to(gameEventsExchange())
            .with("game.move.request");
    }
    
    @Bean
    public Binding moveResponsesBinding() {
        return BindingBuilder
            .bind(moveResponsesQueue())
            .to(gameEventsExchange())
            .with("game.move.response");
    }
    
    @Bean
    public Binding stateUpdatesBinding() {
        return BindingBuilder
            .bind(stateUpdatesQueue())
            .to(gameEventsExchange())
            .with("game.state.updated");
    }
    
    @Bean
    public Binding achievementsBinding() {
        return BindingBuilder
            .bind(achievementsQueue())
            .to(gameEventsExchange())
            .with("game.achievement.unlocked");
    }
    
    @Bean
    public Binding sessionStartRequestedBinding() {
        return BindingBuilder
            .bind(sessionStartRequestedQueue())
            .to(gameEventsExchange())
            .with("game.session.start.requested");
    }
    
    @Bean
    public Binding sessionStartedBinding() {
        return BindingBuilder
            .bind(sessionStartedQueue())
            .to(gameEventsExchange())
            .with("game.session.started");
    }
    
    @Bean
    public Binding sessionEndedBinding() {
        return BindingBuilder
            .bind(sessionEndedQueue())
            .to(gameEventsExchange())
            .with("game.session.ended");
    }
    
    /**
     * Queue for game move applied events.
     * Published by game-service, consumed by platform-backend.
     */
    @Bean
    public Queue moveAppliedQueue() {
        return QueueBuilder
            .durable(moveAppliedQueue)
            .withArgument("x-dead-letter-exchange", dlxName)
            .withArgument("x-dead-letter-routing-key", dlqName)
            .build();
    }
    
    @Bean
    public Binding moveAppliedBinding() {
        return BindingBuilder
            .bind(moveAppliedQueue())
            .to(gameEventsExchange())
            .with("game.move.applied");
    }
    
    // ============================================
    // Chess Game Configuration (External Game)
    // ============================================
    
    @Value("${chess.game.exchange.name:gameExchange}")
    private String chessExchangeName;
    
    @Value("${chess.game.queues.game-created:chess.game.created}")
    private String chessGameCreatedQueue;
    
    @Value("${chess.game.queues.game-updated:chess.game.updated}")
    private String chessGameUpdatedQueue;
    
    @Value("${chess.game.queues.game-ended:chess.game.ended}")
    private String chessGameEndedQueue;
    
    @Value("${chess.game.queues.move-made:chess.move.made}")
    private String chessMoveMadeQueue;
    
    @Value("${chess.game.queues.game-registered:chess.game.registered}")
    private String chessGameRegisteredQueue;
    
    @Value("${chess.game.queues.achievement-acquired:chess.achievement.acquired}")
    private String chessAchievementAcquiredQueue;
    
    /**
     * Chess game exchange (external game service).
     * This is the exchange used by the external chess game.
     */
    @Bean
    public TopicExchange chessGameExchange() {
        return ExchangeBuilder
            .topicExchange(chessExchangeName)
            .durable(true)
            .build();
    }
    
    /**
     * Queue for chess game created events.
     */
    @Bean
    public Queue chessGameCreatedQueue() {
        return QueueBuilder
            .durable(chessGameCreatedQueue)
            .withArgument("x-dead-letter-exchange", dlxName)
            .withArgument("x-dead-letter-routing-key", dlqName)
            .build();
    }
    
    /**
     * Queue for chess game updated events.
     */
    @Bean
    public Queue chessGameUpdatedQueue() {
        return QueueBuilder
            .durable(chessGameUpdatedQueue)
            .withArgument("x-dead-letter-exchange", dlxName)
            .withArgument("x-dead-letter-routing-key", dlqName)
            .build();
    }
    
    /**
     * Queue for chess game ended events.
     */
    @Bean
    public Queue chessGameEndedQueue() {
        return QueueBuilder
            .durable(chessGameEndedQueue)
            .withArgument("x-dead-letter-exchange", dlxName)
            .withArgument("x-dead-letter-routing-key", dlqName)
            .build();
    }
    
    /**
     * Queue for chess move made events.
     */
    @Bean
    public Queue chessMoveMadeQueue() {
        return QueueBuilder
            .durable(chessMoveMadeQueue)
            .withArgument("x-dead-letter-exchange", dlxName)
            .withArgument("x-dead-letter-routing-key", dlqName)
            .build();
    }
    
    /**
     * Queue for chess game registered events.
     */
    @Bean
    public Queue chessGameRegisteredQueue() {
        return QueueBuilder
            .durable(chessGameRegisteredQueue)
            .withArgument("x-dead-letter-exchange", dlxName)
            .withArgument("x-dead-letter-routing-key", dlqName)
            .build();
    }
    
    /**
     * Queue for chess achievement acquired events.
     */
    @Bean
    public Queue chessAchievementAcquiredQueue() {
        return QueueBuilder
            .durable(chessAchievementAcquiredQueue)
            .withArgument("x-dead-letter-exchange", dlxName)
            .withArgument("x-dead-letter-routing-key", dlqName)
            .build();
    }
    
    /**
     * Binding for chess game created events.
     */
    @Bean
    public Binding chessGameCreatedBinding() {
        return BindingBuilder
            .bind(chessGameCreatedQueue())
            .to(chessGameExchange())
            .with("game.created");
    }
    
    /**
     * Binding for chess game updated events.
     */
    @Bean
    public Binding chessGameUpdatedBinding() {
        return BindingBuilder
            .bind(chessGameUpdatedQueue())
            .to(chessGameExchange())
            .with("game.player.names.updated");
    }
    
    /**
     * Binding for chess game ended events.
     */
    @Bean
    public Binding chessGameEndedBinding() {
        return BindingBuilder
            .bind(chessGameEndedQueue())
            .to(chessGameExchange())
            .with("game.ended");
    }
    
    /**
     * Binding for chess move made events.
     */
    @Bean
    public Binding chessMoveMadeBinding() {
        return BindingBuilder
            .bind(chessMoveMadeQueue())
            .to(chessGameExchange())
            .with("move.made");
    }
    
    /**
     * Binding for chess game registered events.
     */
    @Bean
    public Binding chessGameRegisteredBinding() {
        return BindingBuilder
            .bind(chessGameRegisteredQueue())
            .to(chessGameExchange())
            .with("game.registered");
    }
    
    /**
     * Binding for chess achievement acquired events.
     */
    @Bean
    public Binding chessAchievementAcquiredBinding() {
        return BindingBuilder
            .bind(chessAchievementAcquiredQueue())
            .to(chessGameExchange())
            .with("achievement.acquired");
    }
    
    /**
     * JSON message converter for RabbitMQ messages.
     */
    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
    
    /**
     * RabbitMQ template with JSON message converter.
     */
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter());
        template.setMandatory(true);
        return template;
    }
    
    /**
     * Listener container factory with JSON message converter.
     */
    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            ConnectionFactory connectionFactory) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(jsonMessageConverter());
        return factory;
    }
}

