/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package ExchangeObjects;

/**
 *
 * @author Sasa
 */
public class ExchangeException extends Exception {
    private static final long serialVersionUID = 1L;

    /**
     * Creates a new instance of <code>ExchangeException</code> without detail
     * message.
     */
    public ExchangeException() {
    }

    /**
     * Constructs an instance of <code>ExchangeException</code> with the
     * specified detail message.
     *
     * @param msg the detail message.
     */
    public ExchangeException(String msg) {
        super(msg);
    }
}
