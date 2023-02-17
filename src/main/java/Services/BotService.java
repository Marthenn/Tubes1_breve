package Services;

import Enums.ObjectTypes;
import Enums.PlayerActions;
import Models.GameObject;
import Models.GameState;
import Models.PlayerAction;
import Models.Position;

import java.util.Comparator;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;

public class BotService {
    private GameObject bot;
    private PlayerAction playerAction;
    private GameState gameState;
    private boolean afterburner = false;
    private GameObject target = null;
    private static final GameObject worldCenter = new GameObject(UUID.randomUUID(), 0, 0, 0, new Position(), null);
    private boolean teleportFlag = false;
    private UUID teleporterID;
    private final GameObject teleTarget = new GameObject(UUID.randomUUID(), 0, 0, 0, new Position(), null);
    private boolean targetSet = false;

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

    /**
     * Mengomputasikan aksi selanjutnya dari bot
     * @param playerAction instance playerAction yang akan diisi oleh bot
     */
    public void computeNextPlayerAction(PlayerAction playerAction) {
        //Scan for teleporter ID
        if (teleportFlag && teleporterID != null) {
            scanForTeleporter();
        }

        playerAction.action = PlayerActions.FORWARD;
        playerAction.heading = new Random().nextInt(360);
        // Cek apakah ada musuh yang lebih besar dari bot atau ada musuh yang lebih kecil dari bot di sekitarnya
        int protocol = checkProtocol();
        System.out.println("CURRENT PROTOCOL: " + protocol);
        switch (protocol) {
            case 1:
                // Menembak musuh yang lebih kecil dari bot
                var prey = gameState.getPlayerGameObjects()
                        .stream().filter(enemy -> enemy.id != bot.id && getDistanceBetween(enemy, bot) - enemy.getSize() - bot.getSize() <= bot.getSize() * 3 && enemy.getSize() <= bot.getSize())
                        .min(Comparator.comparing(enemy -> getDistanceBetween(bot, enemy)));

                // Tembak torpedoes
                prey.ifPresent(enemy -> {
                    if (bot.getSize() > 40 && bot.getTorpedoCount() > 0) {
                        target = enemy;
                        System.out.println("Torpedoes Shot!");
                        playerAction.action = PlayerActions.FIRETORPEDOES;
                    }
                });
                playerAction.heading = getHeadingBetween(target);
                break;
            case 2:
                // Jika terdapat ancaman player di dekat bot, maka berarah kebalikan dari ancaman tersebut
                var predator = gameState.getPlayerGameObjects()
                        .stream().filter(enemy -> enemy.id != bot.id && getDistanceBetween(enemy, bot) - enemy.getSize() - bot.getSize() <= bot.getSize() + 20 && enemy.getSize() >= bot.getSize())
                        .min(Comparator.comparing(enemy -> getDistanceBetween(bot, enemy)));
                predator.ifPresent(enemy -> {
                    target = enemy;
                    if (bot.getTeleporterCount() > 0 && bot.getSize() > 50) {
                        escape();
                    } else {
                        if (!afterburner && bot.getSize() > 20) {
                            toggleAfterburner();
                        }
                    }
                });
                playerAction.heading = (180 + getHeadingBetween(target)) % 360;
                break;
            case -1:
                // Jika bot berada pada border maka akan bergerak ke tengah
                playerAction.heading = getHeadingBetween(worldCenter);
                target = worldCenter;
            default:
                // Mode pencarian default untuk bot
                playerAction.heading = findTarget();
                break;
        }
        setPlayerAction(playerAction);
    }

    /**
     * Menentukan aksi yang paling efektif untuk bot
     * @return -1 jika berada dekat world border, 1 jika ada musuh yang pantas untuk ditembak, 2 jika ada musuh yang lebih besar, 0 jika tidak ada musuh
     */
    private int checkProtocol() {
        if (getGameState().getWorld().getRadius() != null && getDistanceBetween(bot, worldCenter) >= getGameState().getWorld().getRadius() - (2 * bot.getSize())) {
            return -1;
        } else if (getGameState().getPlayerGameObjects()
                .stream().anyMatch(enemy -> enemy.id != bot.id && getDistanceBetween(enemy, bot) - enemy.getSize() - bot.getSize() <= bot.getSize() * 3 && enemy.getSize() <= 2 * bot.getSize())) {
            return 1;
        } else if (getGameState().getPlayerGameObjects()
                .stream().anyMatch(enemy -> enemy.id != bot.id && getDistanceBetween(enemy, bot) - enemy.getSize() - bot.getSize() <= bot.getSize() * 2 && enemy.getSize() >= bot.getSize())) {
            return 2;
        }
        return 0;
    }

    /**
     * Mengecek jika ada teleporter di sekitar bot
     */
    private void scanForTeleporter() {
        if (!teleportFlag && teleporterID == null) {
            var teleporter = getGameState().getGameObjects()
                    .stream().filter(item -> item.getGameObjectType() == ObjectTypes.TELEPORTER)
                    .min(Comparator.comparing(item -> getDistanceBetween(bot, item)));

            teleporter.ifPresent(tele -> teleporterID = tele.getId());
        }
    }

    /**
     * Mencoba kabur dengan teleporter jika ada
     */
    private void escape() {
        // Cek apakah ada player terdekat yang berbahaya
        var danger = getGameState().getPlayerGameObjects()
                .stream().filter(enemy -> enemy.id != bot.id && getDistanceBetween(enemy, bot) - enemy.getSize() - bot.getSize() <= bot.getSize() + 20 && enemy.getSize() >= bot.getSize())
                .min(Comparator.comparing(enemy -> getDistanceBetween(bot, enemy)));
        // Cek apakah ada player terdekat yang lebih kecil
        var prey = getGameState().getPlayerGameObjects()
                .stream().filter(enemy -> enemy.id != bot.id && enemy.getSize() <= bot.getSize())
                .min(Comparator.comparing(enemy -> getDistanceBetween(bot, enemy)));
        // Cek item di world kecuali bot
        var remaining = getGameState().getPlayerGameObjects()
                .stream().filter(item -> item.id != bot.getId())
                .findAny();

        // Jika ada player yang lebih besar dan berbahaya, maka akan menggunakan teleporter
        if (remaining.isPresent() && danger.isPresent() && !teleportFlag && bot.getSize() - 20 >= danger.get().getSize() / 2) {
            danger.ifPresent(enemy -> {
                System.out.println("ENEMY ALERT!");
                System.out.println("CURRENT SIZE: " + bot.getSize());
                System.out.println("ENEMY SIZE: " + enemy.getSize());
                System.out.println("Salvo amount: " + bot.getTorpedoCount());

                if (prey.isPresent()) {
                    playerAction.heading = getHeadingBetween(prey.get());
                    teleTarget.setPosition(prey.get().getPosition());
                    targetSet = true;
                } else if (getGameState().getWorld().getRadius() != null && (getGameState().getWorld().getRadius() - bot.getPosition().getX() <= bot.getSize() * 2 || getGameState().getWorld().getRadius() - bot.getPosition().getY() <= bot.getSize() * 2)) {
                    System.out.println("SENDING TELEPORTER TO CENTER!");
                    playerAction.heading = getHeadingBetween(worldCenter);
                } else {
                    System.out.println("SENDING TELEPORTER AWAY FROM ENEMY");
                    playerAction.heading = (180 + getHeadingBetween(enemy)) % 360;
                }

                playerAction.action = PlayerActions.FIRETELEPORT;
                System.out.println("TELEPORTER DEPLOYED");
                teleportFlag = true;
            });
        } else if (teleportFlag) {
            if (targetSet) {
                var teleporter = getGameState().getGameObjects()
                        .stream().filter(item -> item.getGameObjectType() == ObjectTypes.TELEPORTER && getDistanceBetween(item, teleTarget) <= bot.getSize())
                        .min(Comparator.comparing(item -> getDistanceBetween(bot, item)));

                teleporter.ifPresent(tele -> {
                    System.out.println("TIME TO TELEPORT!");
                    playerAction.action = PlayerActions.TELEPORT;
                    teleportFlag = false;
                    targetSet = false;
                });
            } else {
                var teleporter = getGameState().getGameObjects()
                        .stream().filter(item -> item.getGameObjectType() == ObjectTypes.TELEPORTER && item.getId() == teleporterID)
                        .min(Comparator.comparing(item -> getDistanceBetween(bot, item)));

                teleporter.ifPresent(teleporterShot -> {
                    var dangerT = getGameState().getPlayerGameObjects()
                            .stream().filter(enemy -> enemy.id != bot.id && getDistanceBetween(enemy, teleporterShot) - enemy.getSize() - bot.getSize() <= bot.getSize() * 2)
                            .min(Comparator.comparing(item -> getDistanceBetween(teleporterShot, item)));

                    dangerT.ifPresentOrElse(item -> System.out.println("Unsafe to teleport"), () -> {
                        System.out.println("TIME TO TELEPORT!");
                        playerAction.action = PlayerActions.TELEPORT;
                        teleportFlag = false;
                        teleporterID = null;
                    });
                });
            }
        }
    }

    /**
     * Mencari target terdekat
     * @return heading yang pantas untuk menghadapi target
     */
    private int findTarget() {
        int heading = 0;
        var nearestFood = getGameState().getGameObjects()
                .stream().filter(item -> item.getGameObjectType() == ObjectTypes.FOOD || item.getGameObjectType() == ObjectTypes.SUPERFOOD)
                .min(Comparator.comparing(item -> getDistanceBetween(bot, item)));
        var nearestGasCloud = getGameState().getGameObjects()
                .stream().filter(gasCloud -> gasCloud.getGameObjectType() == ObjectTypes.GASCLOUD)
                .min(Comparator.comparing(item -> getDistanceBetween(bot, item)));

        if (nearestGasCloud.isPresent() && getDistanceBetween(bot, nearestGasCloud.get()) - nearestGasCloud.get().getSize() < (2 * bot.getSize())) {
            heading = (180 + getHeadingBetween(nearestGasCloud.get())) % 360;
            if (!afterburner && bot.getSize() > 20) {
                toggleAfterburner();
            }
        } else if (nearestFood.isPresent()) {
            if (afterburner) {
                toggleAfterburner();
            }
            heading = getHeadingBetween(nearestFood.get());
            target = nearestFood.get();
        }
        return heading;
    }

    /**
     * Menyalakan atau mematikan afterburner
     */
    private void toggleAfterburner() {
        afterburner ^= true; // Just means toggling the boolean
        playerAction.action = afterburner ? PlayerActions.STARTAFTERBURNER : PlayerActions.STOPAFTERBURNER;
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
