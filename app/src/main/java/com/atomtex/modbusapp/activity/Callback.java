package com.atomtex.modbusapp.activity;

import android.os.Bundle;

/**
 * This interface can be implemented by Activity to make
 * opportunity update its UI by linked 'presenter'.
 * Was designed for implementation of the 'LocalService' and
 * 'MVP' patterns.
 *
 * @author stanislav.kleinikov@gmail.com
 * @see com.atomtex.modbusapp.service.LocalService
 */
public interface Callback {
    void updateUI(Bundle bundle);
}
