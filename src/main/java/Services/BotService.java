package Services;

import Enums.*;
import Models.*;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class BotService {
    private GameObject bot;
    private PlayerAction playerAction;
    private GameState gameState;
    private boolean afterburner = false;
    private GameObject target = null;
    private static final GameObject worldCenter = new GameObject(UUID.randomUUID(), 0, 0, 0, new Position(), null);

    public BotService() {
        this.playerAction = new PlayerAction();
        this.gameState = new GameState();
    }


    public GameObject getBot() {
        return this.bot;
    }

    public void setBot(GameObject bot) {
        this.bot = bot;
    }

    public PlayerAction getPlayerAction() {
        return this.playerAction;
    }

    public void setPlayerAction(PlayerAction playerAction) {
        this.playerAction = playerAction;
    }

    public void computeNextPlayerAction(PlayerAction playerAction) {
//        playerAction.action = PlayerActions.FORWARD;
//        playerAction.heading = new Random().nextInt(360);
//
//        if (!gameState.getGameObjects().isEmpty()) {
//            var foodList = gameState.getGameObjects()
//                    .stream().filter(item -> item.getGameObjectType() == ObjectTypes.FOOD)
//                    .sorted(Comparator
//                            .comparing(item -> getDistanceBetween(bot, item)))
//                    .collect(Collectors.toList());
//
//            playerAction.heading = getHeadingBetween(foodList.get(0));
//        }
//
//        this.playerAction = playerAction;
        playerAction.action = PlayerActions.FORWARD;

        if (!gameState.getGameObjects().isEmpty()) {
            if (target == null) {
                playerAction.heading = findTarget();
            } else {
                var newTarget = gameState.getGameObjects()
                        .stream().filter(item -> item.id == target.id)
                        .findFirst().or(() -> gameState.getPlayerGameObjects()
                                .stream().filter(item -> item.id == target.id)
                                .findFirst());
                newTarget.ifPresentOrElse(target -> {
                    this.target = target;
                    System.out.println("Found new target: " + target);
                    if (target.getSize() < bot.getSize()) {
                        playerAction.heading = getHeadingBetween(target);
                    } else {
                        playerAction.heading = findTarget();
                    }
                }, () -> playerAction.heading = findTarget());
            }
        }

        if (getGameState().getWorld().getRadius() != null && getDistanceBetween(bot, worldCenter) >= getGameState().getWorld().getRadius() - (2 * bot.getSize())) {
            playerAction.heading = getHeadingBetween(worldCenter);
            target = worldCenter;
        }
        this.playerAction = playerAction;
    }

    private int findTarget() {
        AtomicInteger heading = new AtomicInteger();
        var nearestPlayer = gameState.getPlayerGameObjects()
                .stream().filter(item -> item.id != bot.id)
                .min(Comparator.comparing(item -> getDistanceBetween(bot, item)));
        var nearestFood = gameState.getGameObjects()
                .stream().filter(item -> item.getGameObjectType() == ObjectTypes.FOOD)
                .min(Comparator.comparing(item -> getDistanceBetween(bot, item)));
        var nearestGasCloud = gameState.getGameObjects()
                .stream().filter(item -> item.getGameObjectType() == ObjectTypes.GASCLOUD)
                .min(Comparator.comparing(item -> getDistanceBetween(bot, item)));

        nearestPlayer.ifPresent(enemy -> nearestFood.ifPresent(food -> nearestGasCloud.ifPresent(gasCloud -> {
            if (enemy.getSize() < bot.getSize()) {
                if (afterburner) {
                    toggleAfterburner();
                }
                heading.set(getHeadingBetween(enemy));
                target = enemy;
            } else if (enemy.getSize() > bot.getSize()) {
                if (afterburner) {
                    toggleAfterburner();
                }
                if (getDistanceBetween(bot, enemy) + enemy.getSize() < (2 * bot.getSize())) {
                    heading.set(-getHeadingBetween(enemy));
                } else {
                    heading.set(getHeadingBetween(food));
                    target = food;
                }
            } else if (getDistanceBetween(bot, gasCloud) + gasCloud.getSize() < (2 * bot.getSize())) {
                heading.set(-getHeadingBetween(gasCloud));
                if (!afterburner && bot.getSize() > 20) {
                    toggleAfterburner();
                }
            } else {
                if (afterburner) {
                    toggleAfterburner();
                }
                heading.set(getHeadingBetween(food));
                target = food;
            }
        })));

        return heading.get();
    }

    private void toggleAfterburner() {
//        afterburner = !afterburner;
//        playerAction.action = afterburner ? PlayerActions.STARTAFTERBURNER : PlayerActions.STOPAFTERBURNER;
    }

    public GameState getGameState() {
        return this.gameState;
    }

    public void setGameState(GameState gameState) {
        this.gameState = gameState;
        updateSelfState();
    }

    private void updateSelfState() {
        Optional<GameObject> optionalBot = gameState.getPlayerGameObjects().stream().filter(gameObject -> gameObject.id.equals(bot.id)).findAny();
        optionalBot.ifPresent(bot -> this.bot = bot);
    }

    private double getDistanceBetween(GameObject object1, GameObject object2) {
        var triangleX = Math.abs(object1.getPosition().x - object2.getPosition().x);
        var triangleY = Math.abs(object1.getPosition().y - object2.getPosition().y);
        return Math.sqrt(triangleX * triangleX + triangleY * triangleY);
    }

    private int getHeadingBetween(GameObject otherObject) {
        var direction = toDegrees(Math.atan2(otherObject.getPosition().y - bot.getPosition().y,
                otherObject.getPosition().x - bot.getPosition().x));
        return (direction + 360) % 360;
    }

    private int toDegrees(double v) {
        return (int) (v * (180 / Math.PI));
    }
}
