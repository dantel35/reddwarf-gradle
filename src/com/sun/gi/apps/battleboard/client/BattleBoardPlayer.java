/*
 Copyright (c) 2006 Sun Microsystems, Inc., 4150 Network Circle, Santa
 Clara, California 95054, U.S.A. All rights reserved.
 
 Sun Microsystems, Inc. has intellectual property rights relating to
 technology embodied in the product that is described in this document.
 In particular, and without limitation, these intellectual property rights
 may include one or more of the U.S. patents listed at
 http://www.sun.com/patents and one or more additional patents or pending
 patent applications in the U.S. and in other countries.
 
 U.S. Government Rights - Commercial software. Government users are subject
 to the Sun Microsystems, Inc. standard license agreement and applicable
 provisions of the FAR and its supplements.
 
 This distribution may include materials developed by third parties.
 
 Sun, Sun Microsystems, the Sun logo and Java are trademarks or registered
 trademarks of Sun Microsystems, Inc. in the U.S. and other countries.
 
 UNIX is a registered trademark in the U.S. and other countries, exclusively
 licensed through X/Open Company, Ltd.
 
 Products covered by and information contained in this service manual are
 controlled by U.S. Export Control laws and may be subject to the export
 or import laws in other countries. Nuclear, missile, chemical biological
 weapons or nuclear maritime end uses or end users, whether direct or
 indirect, are strictly prohibited. Export or reexport to countries subject
 to U.S. embargo or to entities identified on U.S. export exclusion lists,
 including, but not limited to, the denied persons and specially designated
 nationals lists is strictly prohibited.
 
 DOCUMENTATION IS PROVIDED "AS IS" AND ALL EXPRESS OR IMPLIED CONDITIONS,
 REPRESENTATIONS AND WARRANTIES, INCLUDING ANY IMPLIED WARRANTY OF
 MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE OR NON-INFRINGEMENT,
 ARE DISCLAIMED, EXCEPT TO THE EXTENT THAT SUCH DISCLAIMERS ARE HELD TO BE
 LEGALLY INVALID.
 
 Copyright © 2006 Sun Microsystems, Inc., 4150 Network Circle, Santa Clara,
 California 95054, Etats-Unis. Tous droits réservés.
 
 Sun Microsystems, Inc. détient les droits de propriété intellectuels
 relatifs à la technologie incorporée dans le produit qui est décrit dans
 ce document. En particulier, et ce sans limitation, ces droits de
 propriété intellectuelle peuvent inclure un ou plus des brevets américains
 listés à l'adresse http://www.sun.com/patents et un ou les brevets
 supplémentaires ou les applications de brevet en attente aux Etats -
 Unis et dans les autres pays.
 
 Cette distribution peut comprendre des composants développés par des
 tierces parties.
 
 Sun, Sun Microsystems, le logo Sun et Java sont des marques de fabrique
 ou des marques déposées de Sun Microsystems, Inc. aux Etats-Unis et dans
 d'autres pays.
 
 UNIX est une marque déposée aux Etats-Unis et dans d'autres pays et
 licenciée exlusivement par X/Open Company, Ltd.
 
 see above Les produits qui font l'objet de ce manuel d'entretien et les
 informations qu'il contient sont regis par la legislation americaine en
 matiere de controle des exportations et peuvent etre soumis au droit
 d'autres pays dans le domaine des exportations et importations.
 Les utilisations finales, ou utilisateurs finaux, pour des armes
 nucleaires, des missiles, des armes biologiques et chimiques ou du
 nucleaire maritime, directement ou indirectement, sont strictement
 interdites. Les exportations ou reexportations vers des pays sous embargo
 des Etats-Unis, ou vers des entites figurant sur les listes d'exclusion
 d'exportation americaines, y compris, mais de maniere non exclusive, la
 liste de personnes qui font objet d'un ordre de ne pas participer, d'une
 facon directe ou indirecte, aux exportations des produits ou des services
 qui sont regi par la legislation americaine en matiere de controle des
 exportations et la liste de ressortissants specifiquement designes, sont
 rigoureusement interdites.
 
 LA DOCUMENTATION EST FOURNIE "EN L'ETAT" ET TOUTES AUTRES CONDITIONS,
 DECLARATIONS ET GARANTIES EXPRESSES OU TACITES SONT FORMELLEMENT EXCLUES,
 DANS LA MESURE AUTORISEE PAR LA LOI APPLICABLE, Y COMPRIS NOTAMMENT TOUTE
 GARANTIE IMPLICITE RELATIVE A LA QUALITE MARCHANDE, A L'APTITUDE A UNE
 UTILISATION PARTICULIERE OU A L'ABSENCE DE CONTREFACON.
*/

package com.sun.gi.apps.battleboard.client;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import com.sun.gi.apps.battleboard.BattleBoard;
import com.sun.gi.apps.battleboard.client.swing.Client;
import com.sun.gi.comm.users.client.ClientChannel;
import com.sun.gi.comm.users.client.ClientChannelListener;
import com.sun.gi.comm.users.client.ClientConnectionManager;

public class BattleBoardPlayer implements ClientChannelListener {

    private static Logger log =
        Logger.getLogger("com.sun.gi.apps.battleboard.client");

    private final ClientChannel channel;
    private final ClientConnectionManager connectionManager;
    private Display display;
    private List<String> playerNames = null;
    private List<BattleBoard> playerBoards = null;
    private Map<String, BattleBoard> nameToBoard = null;
    private String myName;
    private BattleBoard myBoard;
    private boolean lost = false;
    private boolean swingMode;

    /**
     * The game play is in one of several states: waiting for a board to
     * be provided by the server (NEED_BOARD), waiting for a player list
     * to be provided by the server (NEED_TURN_ORDER), then iterating
     * through turns (BEGIN_MOVE, END_MOVE), until some player has won
     * (GAME_OVER).
     */
    private enum GameState {
        NEED_BOARD, NEED_TURN_ORDER, BEGIN_MOVE, END_MOVE, GAME_OVER
    }

    private GameState gameState = GameState.NEED_BOARD;

    /**
     * Creates a BattleBoard player instance for the given
     * connectionManager, ClientChannel, and player name.
     * <p>
     * 
     * @param connectionManager the ClientConnectionManager for this
     * game
     * 
     * @param chan the ClientChannel for this game
     * 
     * @param playerName the name of the player
     */
    public BattleBoardPlayer(ClientConnectionManager connectionManager,
            ClientChannel chan, String playerName, boolean swingMode) {
        this.connectionManager = connectionManager;
        this.channel = chan;
        this.myName = playerName;
	this.swingMode = swingMode;

        /*
         * We don't have all the info we need to create the display at
         * this point, so we do not initialize the display yet.
         */

        this.display = null;
    }

    /**
     * {@inheritDoc}
     */
    public void playerJoined(byte[] playerID) {
        log.fine("playerJoined on " + channel.getName());
    }

    /**
     * {@inheritDoc}
     */
    public void playerLeft(byte[] playerID) {
        log.fine("playerLeft on " + channel.getName());

        if (gameState != GameState.GAME_OVER) {
            gameState = GameState.GAME_OVER;
            log.info("Exiting because the other player left");
            connectionManager.disconnect();
            System.exit(-1);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void dataArrived(byte[] uid, ByteBuffer data, boolean reliable) {
        log.fine("dataArrived on " + channel.getName());

        byte[] bytes = new byte[data.remaining()];
        data.get(bytes);
        String text = new String(bytes);

        log.finer("dataArrived: (" + text + ")");

        String[] tokens = text.split("\\s+");
        if (tokens.length == 0) {
            log.warning("empty message");
            return;
        }

        playGame(tokens);
    }

    /**
     * {@inheritDoc}
     */
    public void channelClosed() {
        log.fine("channel " + channel.getName() + " closed");
    }

    /**
     * Performs the game-play for the given array of tokens.
     * 
     * @param tokens an array of Strings containing the tokens of the
     * message from the server
     */
    void playGame(String[] tokens) {
        String cmd = tokens[0];

        if ((gameState == GameState.NEED_BOARD) && "ok".equals(cmd)) {
            gameState = setBoard(tokens);
        } else if ((gameState == GameState.NEED_TURN_ORDER)
                && "turn-order".equals(cmd)) {
            gameState = setTurnOrder(tokens);

            /*
             * At this point, we finally have all the info necessary to
             * build the display.
             */

	    if (swingMode) {
		BattleBoard[] boardArray =
			new BattleBoard[playerBoards.size()];
		display = new Client(playerBoards.toArray(boardArray));
	    } else {
		display = new TextDisplay(playerBoards);
	    }
        } else if ((gameState == GameState.BEGIN_MOVE)
                && "move-started".equals(cmd)) {
            if (myName.equals(tokens[1])) {
                gameState = yourTurn();
            } else {
                gameState = moveStarted(tokens);
            }
            gameState = GameState.END_MOVE;
        } else if ((gameState == GameState.END_MOVE)
                && "move-ended".equals(cmd)) {
            gameState = moveEnded(tokens);
        } else {
            log.severe("Illegal game state: cmd " + cmd + " gameState "
                    + gameState);
        }

        /*
         * If there's only one player left, that last remaining player
         * is the winner.
         */

        if (playerBoards != null && playerBoards.size() == 1) {
            if (myName.equals(playerBoards.get(0).getPlayerName())) {
                display.message("YOU WIN!");
            } else {
                display.message(playerBoards.get(0).getPlayerName() + " WINS!");
            }
            gameState = GameState.GAME_OVER;
            connectionManager.disconnect();
            display.gameOver();
        }
    }

    /**
     * Implements the operations for the "ok" message, which tells the
     * user what board the server has chosen for them.
     * 
     * @param args an array of Strings containing the tokens of the
     * message from the server
     * 
     * @return <code>true</code> if the message was valid and executed
     * correctly, <code>false</code> otherwise
     */
    private GameState setBoard(String[] args) {
        if (args.length < 4) {
            log.severe("setBoard: incorrect number of arguments");
            return GameState.NEED_BOARD;
        }

        int boardWidth = Integer.parseInt(args[1]);
        int boardHeight = Integer.parseInt(args[2]);
        int numCities = Integer.parseInt(args[3]);

        if ((boardWidth < 1) || (boardHeight < 1)) {
            log.severe("bad board dimensions (" + boardWidth + ", "
                    + boardHeight + ")");
            return GameState.NEED_BOARD;
        }

        if (numCities < 1) {
            log.severe("bad numCities (" + numCities + ")");
            return GameState.NEED_BOARD;
        }

        BattleBoard tempBoard = new BattleBoard(myName, boardWidth,
                boardHeight, numCities);

        if (((args.length - 4) % 2) != 0) {
            log.severe("bad list of city positions");
            return GameState.NEED_BOARD;
        }

        for (int base = 4; base < args.length; base += 2) {
            int x = Integer.parseInt(args[base]);
            int y = Integer.parseInt(args[base + 1]);

            if ((x < 0) || (x >= boardWidth) || (y < 0) || (y >= boardHeight)) {
                log.severe("improper city position (" + x + ", " + y + ")");
                return GameState.NEED_BOARD;
            }

            tempBoard.update(x, y, BattleBoard.PositionValue.CITY);
        }
        myBoard = tempBoard;

        return GameState.NEED_TURN_ORDER;
    }

    /**
     * Implements the operations for the "turn-order" message, which
     * tells the player what the order of turns will be among the
     * players.
     * 
     * @param args an array of Strings containing the tokens of the
     * message from the server
     * 
     * @return <code>BEGIN_MOVE</code> if the message was valid and
     * executed correctly and moves may begin,,
     * <code>NEED_TURN_ORDER</code> otherwise
     */
    private GameState setTurnOrder(String[] args) {

        if (playerBoards != null) {
            log.severe("setTurnOrder has already been done");
            return GameState.NEED_TURN_ORDER;
        }

        if (args.length < 3) {
            log.severe("setTurnOrder: " + "incorrect number of args: "
                    + args.length + " != 3");
            return GameState.NEED_TURN_ORDER;
        }

        playerNames = new LinkedList<String>();
        playerBoards = new LinkedList<BattleBoard>();
        nameToBoard = new HashMap<String, BattleBoard>();

        for (int i = 1; i < args.length; i++) {
            String playerName = args[i];
            playerNames.add(playerName);

            if (myName.equals(playerName)) {
                playerBoards.add(myBoard);
                nameToBoard.put(myName, myBoard);
            } else {
                BattleBoard newBoard = new BattleBoard(playerName,
                        myBoard.getWidth(), myBoard.getHeight(),
                        myBoard.getStartCities());
                playerBoards.add(newBoard);
                nameToBoard.put(playerName, newBoard);
            }
        }

        return GameState.BEGIN_MOVE;
    }

    /**
     * Implements the operations for the "move-started" message, for the
     * player whose move it is.
     * <p>
     * 
     * Note that the {@link Display#getMove getMove} is responsible for
     * validating the input, and therefore there is no checking here. If
     * the client sends a bad move to the server, the server will detect
     * the error and substitute a "pass".
     * <p>
     * 
     * @return <code>END_MOVE</code> if the move was executed
     * correctly, <code>BEGIN_MOVE</code> otherwise
     */
    private GameState yourTurn() {
        display.showBoards(myName);
        display.message("Your move!");

        String[] move = display.getMove();

        if ((move.length == 1) && "pass".equals(move[0])) {
            ByteBuffer buf = ByteBuffer.wrap("pass".getBytes());
            buf.position(buf.limit());
            connectionManager.sendToServer(buf, true);
        } else if (move.length == 3) {
            String bombedPlayer = move[0];
            int x = Integer.parseInt(move[1]);
            int y = Integer.parseInt(move[2]);

            String moveMessage = "move " + bombedPlayer + " " + x + " " + y;

            ByteBuffer buf = ByteBuffer.wrap(moveMessage.getBytes());
            buf.position(buf.limit());
            connectionManager.sendToServer(buf, true);
        } else {
            display.message("Improper move.  Passing instead....");
            ByteBuffer buf = ByteBuffer.wrap("pass".getBytes());
            buf.position(buf.limit());
            connectionManager.sendToServer(buf, true);
        }
        return GameState.END_MOVE;
    }

    /**
     * Implements the operations for the "move-started" message, for any
     * player whose move it is not.
     * 
     * @param args an array of Strings containing the tokens of the
     * message from the server
     * 
     * @return <code>END_MOVE</code> if the move was executed
     * correctly, <code>BEGIN_MOVE</code> otherwise
     */
    private GameState moveStarted(String[] args) {

        if (args.length != 2) {
            log.severe("moveStarted: " + "incorrect number of args: "
                    + args.length + " != 2");
            return GameState.BEGIN_MOVE;
        }

        String currPlayer = args[1];
        log.fine("move-started for " + currPlayer);

        if (!playerNames.contains(currPlayer)) {
            log.severe("moveStarted: nonexistant player (" + currPlayer + ")");
            return GameState.BEGIN_MOVE;
        }

        display.showBoards(currPlayer);
        display.message(currPlayer + " is making a move...");
        return GameState.END_MOVE;
    }

    /**
     * Implements the operations for the "move-ended" message.
     * 
     * @param args an array of Strings containing the tokens of the
     * message from the server
     * 
     * @return <code>BEGIN_MOVE</code> if the move was executed
     * correctly, <code>END_MOVE</code> otherwise
     */
    private GameState moveEnded(String[] args) {

        if (args.length < 3) {
            log.severe("moveEnded: " + "incorrect number of args: "
                    + args.length + " < 3");
            return GameState.END_MOVE;
        }

        String currPlayer = args[1];
        String action = args[2];

        log.fine("move-ended for " + currPlayer);

        if ("pass".equals(action)) {
            if (args.length != 3) {
                log.severe("moveEnded: " + "incorrect number of args: "
                        + args.length + " != 3");
                return GameState.END_MOVE;
            }
            log.fine(currPlayer + " passed");

            display.message(currPlayer + " passed.");
            return GameState.BEGIN_MOVE;
        } else if ("bomb".equals(action)) {
            if (args.length != 7) {
                log.severe("moveEnded: " + "incorrect number of args: "
                        + args.length + " != 7");
                return GameState.END_MOVE;
            }

            String bombedPlayer = args[3];
            BattleBoard board = nameToBoard.get(bombedPlayer);
            if (board == null) {
                log.severe("nonexistant player (" + bombedPlayer + ")");
                return GameState.END_MOVE;
            }

            int x = Integer.parseInt(args[4]);
            int y = Integer.parseInt(args[5]);

            if ((x < 0) || (x >= myBoard.getWidth()) || (y < 0)
                    || (y >= myBoard.getHeight())) {
                log.warning("impossible board position " + "(" + x + ", " +
			y + ")");
                return GameState.END_MOVE;
            }

            String outcome = args[6];

            log.fine(bombedPlayer + " bombed (" + x + ", " + y
                    + ") with outcome " + outcome);
            display.message(currPlayer + " bombed " + bombedPlayer + " at " +
		    x + "," + y + " with outcome " + outcome);

            if ("HIT".equals(outcome) || "LOSS".equals(outcome)) {
                board.update(x, y, BattleBoard.PositionValue.HIT);
                board.hit();

                if ("LOSS".equals(outcome)) {
                    playerBoards.remove(nameToBoard.get(bombedPlayer));
                    playerNames.remove(bombedPlayer);
                    display.removePlayer(bombedPlayer);
                    if (bombedPlayer.equals(myName)) {
                        display.message("You lose!");
                        display.message("Better luck next time.");
                        lost = true;
                    } else {
                        display.message(bombedPlayer +
				" lost their last city.");
                    }
                } else {
                    if (bombedPlayer.equals(myName)) {
                        display.message("You just lost a city!");
                    } else {
                        display.message(bombedPlayer + " lost a city.");
                    }
                }
            } else if ("NEAR_MISS".equals(outcome)) {
                board.update(x, y, BattleBoard.PositionValue.NEAR);
            } else if ("MISS".equals(outcome)) {
                board.update(x, y, BattleBoard.PositionValue.MISS);
            }

            display.showBoards(bombedPlayer);
        } else {
            log.severe("moveEnded: invalid command");
            return GameState.END_MOVE;
        }

        return GameState.BEGIN_MOVE;
    }

    /**
     * Implements the operations for the "withdraw" message.
     * <p>
     * 
     * <em>This method is not used in the current game.</em>
     * 
     * @param args an array of Strings containing the tokens of the
     * message from the server
     * 
     * @return <code>true</code> if the move was executed correctly,
     * <code>false</code> otherwise
     */
    private boolean withdraw(String[] args) {
        if (playerBoards == null) {
            log.severe("setTurnOrder has not yet been done");
            return false;
        }

        if (args.length != 2) {
            log.severe("withdraw: incorrect number of args: " + args.length
                    + " != 2");
            return false;
        }

        String withdrawnPlayer = args[1];
        if (!playerNames.remove(withdrawnPlayer)) {
            log.warning("withdraw: nonexistant player (" + withdrawnPlayer
                    + ")");
            return false;
        } else {
            log.fine(withdrawnPlayer + " has withdrawn");

            display.removePlayer(withdrawnPlayer);
            display.showBoards(null);
            display.message(withdrawnPlayer + " has withdrawn.");
        }

        return true;
    }

    /**
     * Returns <code>true</code> if this player has lost the game,
     * <code>false</code> otherwise.
     * 
     * @return <code>true</code> if this player has lost the game,
     * <code>false</code> otherwise
     */
    public boolean lost() {
        return lost;
    }
}