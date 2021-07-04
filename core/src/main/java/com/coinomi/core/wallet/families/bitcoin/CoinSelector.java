package com.coinomi.core.wallet.families.bitcoin;

import org.bitcoinj.core.Coin;
import org.bitcoinj.wallet.DefaultCoinSelector;

import java.util.List;

/**
 * A CoinSelector is responsible for picking some outputs to spend, from the list of all spendable outputs. It
 * allows you to customize the policies for creation of transactions to suit your needs. The select operation
 * may return a {@