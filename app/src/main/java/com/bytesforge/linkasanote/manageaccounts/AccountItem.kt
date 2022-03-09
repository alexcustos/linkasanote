/*
 * LaaNo Android application
 *
 * @author Aleksandr Borisenko <developer@laano.net>
 * Copyright (C) 2017 Aleksandr Borisenko
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.bytesforge.linkasanote.manageaccounts

import android.accounts.Account
import com.google.common.base.Objects

class AccountItem {
    var account: Account? = null
        private set
    var displayName: String? = null
    var type: Int
        private set

    constructor(account: Account) {
        this.account = account
        type = TYPE_ACCOUNT
    }

    constructor() {
        type = TYPE_ACTION_ADD
    }

    val accountName: String?
        get() = if (account == null) null else account!!.name

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || javaClass != other.javaClass) return false

        val accountItem = other as AccountItem
        if ((account == null) xor (accountItem.account == null)) return false

        // NOTE: (account == null && accountItem.account == null)
        return ((account == null || Objects.equal(account!!.name, accountItem.account!!.name))
                && Objects.equal(displayName, accountItem.displayName))
    }

    override fun hashCode(): Int {
        return Objects.hashCode(account, displayName, type)
    }

    companion object {
        const val TYPE_ACCOUNT = 0
        const val TYPE_ACTION_ADD = 1
    }
}