import { HardhatUserConfig } from "hardhat/config";
import "@nomicfoundation/hardhat-toolbox-viem";
import "hardhat-gas-reporter";

const config: HardhatUserConfig = {
  solidity: "0.8.20",
  gasReporter: {
    currency: 'USD',
    // enabled: true
    // enabled: (process.env.REPORT_GAS) ? true : false
  }
};

export default config;
