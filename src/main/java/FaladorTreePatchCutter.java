import rsb.event.listener.PaintListener;
import rsb.methods.*;
import rsb.script.Script;
import rsb.script.ScriptManifest;
import rsb.wrappers.RSArea;
import rsb.wrappers.RSNPC;
import rsb.wrappers.RSObject;
import rsb.wrappers.RSTile;

import java.awt.*;
import java.util.Arrays;
import java.util.Locale;

import static rsb.methods.Bank.BANK_BOOTHS;
import static rsb.methods.MethodProvider.methods;


@ScriptManifest(authors = { "dginovker" }, name = "Falador Tree Patch Cutter", version = 1.04, description = "<html><head>"
        + "</head><body>"
        + "<center><strong><h2>dginovker's Falador Tree Patch Cutter</h2></strong></center>"
        + "<center><strong>Start the script in Falador by the Tree Patch with an axe and a tree ready<br />"
        + "</body></html>")
public class FaladorTreePatchCutter extends Script implements PaintListener {
    private long startTime;
    private State lastState;

    private void l(String toLog) {
        log("Bracket: " + toLog);
    }

    private enum State {
        goingToBank,
        openingBank,
        bankingItems,
        walkingToPatch,
        waitingForTree,
        afkingBeforeCuttingTree,
        clickingTree,
        cuttingTree,
        unknown
    }

    final ScriptManifest properties = getClass().getAnnotation(
            ScriptManifest.class);

    private final RSArea outsideBank = new RSArea(new RSTile(3008, 3362), new RSTile(3012, 3360));
    private final RSArea aroundPatch = new RSArea(new RSTile(3001, 3375), new RSTile(3002, 3371));
    private final RSTile treeTile = new RSTile(3004, 3373);
    private final int treePatchId = 8389;
    private final int[] treeInPatchVarbits = {
            46 // yew tree
    };
    private int startXP = 0;
    private int startLvl = 0;

    private State getState() {
        if (players.getMyPlayer().getAnimation() == 867) {
            return State.cuttingTree;
        }
        if (methods.bank.isOpen()) {
            return State.bankingItems;
        }
        int canSeeBankY = 3362;
        if (methods.players.getMyPlayer().getLocation().getY() <= canSeeBankY) {
            return State.openingBank;
        }
        if (lastState != State.openingBank && getTree() == null && methods.inventory.getCount() > 20 || methods.inventory.getCount() >= 28) {
            return State.goingToBank;
        }
        if (methods.inventory.getCount() <= 20 && methods.calc.distanceTo(treeTile) > 5) {
            return State.walkingToPatch;
        }
        if (getTree() == null && methods.calc.distanceTo(treeTile) <= 5) {
            return State.waitingForTree;
        }
        if (lastState == State.afkingBeforeCuttingTree && getTree() != null) {
            return State.clickingTree;
        }
        if (getTree() != null && methods.calc.distanceTo(treeTile) <= 5) {
            return State.afkingBeforeCuttingTree;
        }


        return State.unknown;
    }

    private RSObject getTree() {
        RSObject patchObj = methods.objects.getNearest(t -> t != null
            && t.getID() == treePatchId
        );


        int faladorPatchVarpId = 529;
        int faladorPatchVarp = clientLocalStorage.getVarpValueAt(faladorPatchVarpId);

        for (int treeInPatchVarbit : treeInPatchVarbits) {
            if (faladorPatchVarp == treeInPatchVarbit) {
                l("There's a tree in the patch!");
                return patchObj;
            }
        }
        l("No tree found");
        return null;
    }

    @Override
    public int loop() {
        if (!game.isLoggedIn()) {
            return 2000;
        }

        lastState = getState();
        switch (lastState) {
            case goingToBank:
                if (players.getMyPlayer().isLocalPlayerMoving()) {
                    return 900;
                }
                walking.walkTo(outsideBank.getRandomTile());
                break;
            case openingBank:
                RSObject bankBooth = methods.objects.getNearest(BANK_BOOTHS);
                if (!bankBooth.isOnScreen()) {
                    camera.turnTo(bankBooth);
                }
                bankBooth.doClick();
                break;
            case bankingItems:
                bank.depositAll();
                break;
            case walkingToPatch:
                walking.walkTo(aroundPatch.getRandomTile());
                break;
            case waitingForTree:
                return 300;
            case afkingBeforeCuttingTree:
                return 1000 * random(1, 10);
            case clickingTree:
                RSObject tree = getTree();
                if (tree != null) {
                    ctx.tiles.doAction(tree.getLocation(), "Chop down");
                    sleep(random(0, 2000));
                    mouse.moveOffScreen();
                }
                break;
            case cuttingTree:
                return 300;
            case unknown:
                return 300;
            default:
                l("Unknown state");
        }

        return 3000;
    }

    @Override
    public void onFinish() {
        getBot().getEventManager().removeListener(this);
    }

    @Override
    public boolean onStart() {
        startTime = System.currentTimeMillis();
        startLvl = skills.getCurrentLevel(Skills.getIndex("woodcutting"));
        startXP = skills.getCurrentExp(Skills.getIndex("woodcutting"));
        mouse.setSpeed(mouse.getSpeed() * 8);
        l("Starting script with mouse speed of " + mouse.getSpeed());
        return true;
    }

    public void onRepaint(final Graphics g) {
        // Draw bot's mouse
        if (methods.mouse.isPressed()) {
            g.setColor(new Color(255, 255, 0, 175));
            g.fillOval(methods.mouse.getLocation().getX(), methods.mouse.getLocation().getY(), 7, 7);
            g.setColor(new Color(0, 255, 255, 175));
            g.drawOval(methods.mouse.getLocation().getX(), methods.mouse.getLocation().getY(), 7, 7);
        } else {
            g.setColor(new Color(0, 255, 255, 175));
            g.fillOval(methods.mouse.getLocation().getX(), methods.mouse.getLocation().getY(), 7, 7);
            g.setColor(new Color(255, 255, 0, 175));
            g.drawOval(methods.mouse.getLocation().getX(), methods.mouse.getLocation().getY(), 7, 7);
        }
        long runTime = 0;
        long seconds = 0;
        long minutes = 0;
        long hours = 0;
        int currentXP = 0;
        int currentLVL = 0;
        int gainedXP = 0;

        runTime = System.currentTimeMillis() - startTime;
        seconds = runTime / 1000;
        if (seconds >= 60) {
            minutes = seconds / 60;
            seconds -= minutes * 60;
        }
        if (minutes >= 60) {
            hours = minutes / 60;
            minutes -= hours * 60;
        }

        g.setColor(new Color(0, 255, 255, 175));
        if (mouse.isPressed()) {
            g.setColor(new Color(255, 255, 0, 175));
        }
        g.fillOval(mouse.getLocation().getX(), mouse.getLocation().getY(), 7, 7);

        currentLVL = skills.getCurrentLevel(Skills
                .getIndex("woodcutting"));
        currentXP = skills.getCurrentExp(Skills.getIndex("woodcutting"));
        gainedXP = currentXP - startXP;

        if (game.getCurrentTab() == GameGUI.Tab.INVENTORY) {
            g.setColor(new Color(0, 0, 0, 175));
            g.fillRoundRect(555, 210, 175, 250, 10, 10);
            g.setColor(Color.WHITE);
            final int[] coords = new int[]{225, 240, 255, 270, 285, 300, 315,
                    330, 345, 360, 375, 390, 405, 420, 435, 450};
            g.drawString(properties.name(), 561, coords[0]);
            g.drawString("Version: " + properties.version(), 561, coords[1]);
            g.drawString("Run Time: " + hours + ":" + minutes + ":" + seconds,
                    561, coords[2]);
            g.drawString("Current Lvl: " + currentLVL, 561, coords[4]);
            g.drawString("Lvls Gained: " + (currentLVL - startLvl), 561,
                    coords[5]);
            g.drawString("XP Gained: " + gainedXP, 561, coords[6]);
            g.drawString("% To Next Level: "
                    + skills.getPercentToNextLevel(Skills
                    .getIndex("woodcutting")), 561, coords[8]);

            g.drawString("State: " + lastState, 561, coords[10]);
            g.drawString("Animation: " + players.getMyPlayer().getAnimation(), 561, coords[11]);
        }
    }
}
