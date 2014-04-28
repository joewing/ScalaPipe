//*****************************************************************************
// (c) Copyright 2009 Xilinx, Inc. All rights reserved.
//
// This file contains confidential and proprietary information
// of Xilinx, Inc. and is protected under U.S. and
// international copyright and other intellectual property
// laws.
//
// DISCLAIMER
// This disclaimer is not a license and does not grant any
// rights to the materials distributed herewith. Except as
// otherwise provided in a valid license issued to you by
// Xilinx, and to the maximum extent permitted by applicable
// law: (1) THESE MATERIALS ARE MADE AVAILABLE "AS IS" AND
// WITH ALL FAULTS, AND XILINX HEREBY DISCLAIMS ALL WARRANTIES
// AND CONDITIONS, EXPRESS, IMPLIED, OR STATUTORY, INCLUDING
// BUT NOT LIMITED TO WARRANTIES OF MERCHANTABILITY, NON-
// INFRINGEMENT, OR FITNESS FOR ANY PARTICULAR PURPOSE; and
// (2) Xilinx shall not be liable (whether in contract or tort,
// including negligence, or under any other theory of
// liability) for any loss or damage of any kind or nature
// related to, arising under or in connection with these
// materials, including for any direct, or any indirect,
// special, incidental, or consequential loss or damage
// (including loss of data, profits, goodwill, or any type of
// loss or damage suffered as a result of any action brought
// by a third party) even if such damage or loss was
// reasonably foreseeable or Xilinx had been advised of the
// possibility of the same.
//
// CRITICAL APPLICATIONS
// Xilinx products are not designed or intended to be fail-
// safe, or for use in any application requiring fail-safe
// performance, such as life-support or safety devices or
// systems, Class III medical devices, nuclear facilities,
// applications related to the deployment of airbags, or any
// other applications that could lead to death, personal
// injury, or severe property or environmental damage
// (individually and collectively, "Critical
// Applications"). Customer assumes the sole risk and
// liability of any use of Xilinx products in Critical
// Applications, subject only to applicable laws and
// regulations governing limitations on product liability.
//
// THIS COPYRIGHT NOTICE AND DISCLAIMER MUST BE RETAINED AS
// PART OF THIS FILE AT ALL TIMES.
//
//*****************************************************************************
//   ____  ____
//  /   /\/   /
// /___/  \  /    Vendor             : Xilinx
// \   \   \/     Version            : 3.92
//  \   \         Application        : MIG
//  /   /         Filename           : mig_lpddr #.v
// /___/   /\     Date Last Modified : $Date: 2011/06/02 07:17:09 $
// \   \  /  \    Date Created       : Tue Feb 23 2010
//  \___\/\___\
//
//Device           : Spartan-6
//Design Name      : DDR/DDR2/DDR3/LPDDR 
//Purpose          : This is a template file for the design top module. This module contains 
//                   all the four memory controllers and the two infrastructures. However,
//                   only the enabled modules will be active and others inactive.
//Reference        :
//Revision History :
//*****************************************************************************
`timescale 1ns/1ps

(* X_CORE_INFO = "mig_v3_92_lpddr_lpddr_s6, Coregen 14.7" , CORE_GENERATION_INFO = "lpddr_lpddr_s6,mig_v3_92,{component_name=mig_lpddr, C3_MEM_INTERFACE_TYPE=LPDDR, C3_CLK_PERIOD=10000, C3_MEMORY_PART=mt46h32m16xxxx-5, C3_MEMORY_DEVICE_WIDTH=16, C3_PA_SR=FULL, C3_OUTPUT_DRV=FULL, C3_PORT_CONFIG=One 128-bit bi-directional port, C3_MEM_ADDR_ORDER=ROW_BANK_COLUMN, C3_PORT_ENABLE=Port0, C3_INPUT_PIN_TERMINATION=EXTERN_TERM, C3_DATA_TERMINATION=25 Ohms, C3_CLKFBOUT_MULT_F=4, C3_CLKOUT_DIVIDE=2, C3_DEBUG_PORT=0, C3_INPUT_CLK_TYPE=Single-Ended, LANGUAGE=Verilog, SYNTHESIS_TOOL=Foundation_ISE, NO_OF_CONTROLLERS=1}" *)
module mig_lpddr #
(
   parameter C3_P0_MASK_SIZE           = 16,
   parameter C3_P0_DATA_PORT_SIZE      = 128,
   parameter DEBUG_EN                = 0,       
                                       // # = 1, Enable debug signals/controls,
                                       //   = 0, Disable debug signals/controls.
   parameter C3_MEMCLK_PERIOD        = 10000,       
                                       // Memory data transfer clock period
   parameter C3_CALIB_SOFT_IP        = "TRUE",       
                                       // # = TRUE, Enables the soft calibration logic,
                                       // # = FALSE, Disables the soft calibration logic.
   parameter C3_SIMULATION           = "FALSE",       
                                       // # = TRUE, Simulating the design. Useful to reduce the simulation time,
                                       // # = FALSE, Implementing the design.
   parameter C3_RST_ACT_LOW          = 0,       
                                       // # = 1 for active low reset,
                                       // # = 0 for active high reset.
   parameter C3_INPUT_CLK_TYPE       = "SINGLE_ENDED",       
                                       // input clock type DIFFERENTIAL or SINGLE_ENDED
   parameter C3_MEM_ADDR_ORDER       = "ROW_BANK_COLUMN",       
                                       // The order in which user address is provided to the memory controller,
                                       // ROW_BANK_COLUMN or BANK_ROW_COLUMN
   parameter C3_NUM_DQ_PINS          = 16,       
                                       // External memory data width
   parameter C3_MEM_ADDR_WIDTH       = 13,       
                                       // External memory address width
   parameter C3_MEM_BANKADDR_WIDTH   = 2        
                                       // External memory bank address width
)	

(

   inout  [C3_NUM_DQ_PINS-1:0]                      mcb3_dram_dq,
   output [C3_MEM_ADDR_WIDTH-1:0]                   mcb3_dram_a,
   output [C3_MEM_BANKADDR_WIDTH-1:0]               mcb3_dram_ba,
   output                                           mcb3_dram_cke,
   output                                           mcb3_dram_ras_n,
   output                                           mcb3_dram_cas_n,
   output                                           mcb3_dram_we_n,
   output                                           mcb3_dram_dm,
   inout                                            mcb3_dram_udqs,
   inout                                            mcb3_rzq,
   output                                           mcb3_dram_udm,
   input                                            c3_sys_clk,
   input                                            c3_sys_rst_i,
   output                                           c3_calib_done,
   output                                           c3_clk0,
   output                                           c3_rst0,
   inout                                            mcb3_dram_dqs,
   output                                           mcb3_dram_ck,
   output                                           mcb3_dram_ck_n,
      input		c3_p0_cmd_clk,
      input		c3_p0_cmd_en,
      input [2:0]	c3_p0_cmd_instr,
      input [5:0]	c3_p0_cmd_bl,
      input [29:0]	c3_p0_cmd_byte_addr,
      output		c3_p0_cmd_empty,
      output		c3_p0_cmd_full,
      input		c3_p0_wr_clk,
      input		c3_p0_wr_en,
      input [C3_P0_MASK_SIZE - 1:0]	c3_p0_wr_mask,
      input [C3_P0_DATA_PORT_SIZE - 1:0]	c3_p0_wr_data,
      output		c3_p0_wr_full,
      output		c3_p0_wr_empty,
      output [6:0]	c3_p0_wr_count,
      output		c3_p0_wr_underrun,
      output		c3_p0_wr_error,
      input		c3_p0_rd_clk,
      input		c3_p0_rd_en,
      output [C3_P0_DATA_PORT_SIZE - 1:0]	c3_p0_rd_data,
      output		c3_p0_rd_full,
      output		c3_p0_rd_empty,
      output [6:0]	c3_p0_rd_count,
      output		c3_p0_rd_overflow,
      output		c3_p0_rd_error
);
// The parameter CX_PORT_ENABLE shows all the active user ports in the design.
// For example, the value 6'b111100 tells that only port-2, port-3, port-4
// and port-5 are enabled. The other two ports are inactive. An inactive port
// can be a disabled port or an invisible logical port. Few examples to the 
// invisible logical port are port-4 and port-5 in the user port configuration,
// Config-2: Four 32-bit bi-directional ports and the ports port-2 through
// port-5 in Config-4: Two 64-bit bi-directional ports. Please look into the 
// Chapter-2 of ug388.pdf in the /docs directory for further details.
   localparam  C3_P1_MASK_SIZE           =16;
   localparam  C3_P1_DATA_PORT_SIZE      =128;
   localparam C3_PORT_ENABLE              = 6'b000001;
   localparam C3_PORT_CONFIG             =  "B128";
   localparam C3_CLKOUT0_DIVIDE       = 2;       
   localparam C3_CLKOUT1_DIVIDE       = 2;       
   localparam C3_CLKOUT2_DIVIDE       = 4;       
   localparam C3_CLKOUT3_DIVIDE       = 8;       
   localparam C3_CLKFBOUT_MULT        = 4;       
   localparam C3_DIVCLK_DIVIDE        = 1;       
   localparam C3_ARB_ALGORITHM        = 0;       
   localparam C3_ARB_NUM_TIME_SLOTS   = 12;       
   localparam C3_ARB_TIME_SLOT_0      = 3'o0;       
   localparam C3_ARB_TIME_SLOT_1      = 3'o0;       
   localparam C3_ARB_TIME_SLOT_2      = 3'o0;       
   localparam C3_ARB_TIME_SLOT_3      = 3'o0;       
   localparam C3_ARB_TIME_SLOT_4      = 3'o0;       
   localparam C3_ARB_TIME_SLOT_5      = 3'o0;       
   localparam C3_ARB_TIME_SLOT_6      = 3'o0;       
   localparam C3_ARB_TIME_SLOT_7      = 3'o0;       
   localparam C3_ARB_TIME_SLOT_8      = 3'o0;       
   localparam C3_ARB_TIME_SLOT_9      = 3'o0;       
   localparam C3_ARB_TIME_SLOT_10     = 3'o0;       
   localparam C3_ARB_TIME_SLOT_11     = 3'o0;       
   localparam C3_MEM_TRAS             = 40000;       
   localparam C3_MEM_TRCD             = 15000;       
   localparam C3_MEM_TREFI            = 7800000;       
   localparam C3_MEM_TRFC             = 97500;       
   localparam C3_MEM_TRP              = 15000;       
   localparam C3_MEM_TWR              = 15000;       
   localparam C3_MEM_TRTP             = 7500;       
   localparam C3_MEM_TWTR             = 2;       
   localparam C3_MEM_TYPE             = "MDDR";       
   localparam C3_MEM_DENSITY          = "512Mb";       
   localparam C3_MEM_BURST_LEN        = 8;       
   localparam C3_MEM_CAS_LATENCY      = 3;       
   localparam C3_MEM_NUM_COL_BITS     = 10;       
   localparam C3_MEM_DDR1_2_ODS       = "FULL";       
   localparam C3_MEM_DDR2_RTT         = "150OHMS";       
   localparam C3_MEM_DDR2_DIFF_DQS_EN  = "YES";       
   localparam C3_MEM_DDR2_3_PA_SR     = "FULL";       
   localparam C3_MEM_DDR2_3_HIGH_TEMP_SR  = "NORMAL";       
   localparam C3_MEM_DDR3_CAS_LATENCY  = 6;       
   localparam C3_MEM_DDR3_ODS         = "DIV6";       
   localparam C3_MEM_DDR3_RTT         = "DIV2";       
   localparam C3_MEM_DDR3_CAS_WR_LATENCY  = 5;       
   localparam C3_MEM_DDR3_AUTO_SR     = "ENABLED";       
   localparam C3_MEM_MOBILE_PA_SR     = "FULL";       
   localparam C3_MEM_MDDR_ODS         = "FULL";       
   localparam C3_MC_CALIB_BYPASS      = "NO";       
   localparam C3_MC_CALIBRATION_MODE  = "CALIBRATION";       
   localparam C3_MC_CALIBRATION_DELAY  = "HALF";       
   localparam C3_SKIP_IN_TERM_CAL     = 1;       
   localparam C3_SKIP_DYNAMIC_CAL     = 0;       
   localparam C3_LDQSP_TAP_DELAY_VAL  = 0;       
   localparam C3_LDQSN_TAP_DELAY_VAL  = 0;       
   localparam C3_UDQSP_TAP_DELAY_VAL  = 0;       
   localparam C3_UDQSN_TAP_DELAY_VAL  = 0;       
   localparam C3_DQ0_TAP_DELAY_VAL    = 0;       
   localparam C3_DQ1_TAP_DELAY_VAL    = 0;       
   localparam C3_DQ2_TAP_DELAY_VAL    = 0;       
   localparam C3_DQ3_TAP_DELAY_VAL    = 0;       
   localparam C3_DQ4_TAP_DELAY_VAL    = 0;       
   localparam C3_DQ5_TAP_DELAY_VAL    = 0;       
   localparam C3_DQ6_TAP_DELAY_VAL    = 0;       
   localparam C3_DQ7_TAP_DELAY_VAL    = 0;       
   localparam C3_DQ8_TAP_DELAY_VAL    = 0;       
   localparam C3_DQ9_TAP_DELAY_VAL    = 0;       
   localparam C3_DQ10_TAP_DELAY_VAL   = 0;       
   localparam C3_DQ11_TAP_DELAY_VAL   = 0;       
   localparam C3_DQ12_TAP_DELAY_VAL   = 0;       
   localparam C3_DQ13_TAP_DELAY_VAL   = 0;       
   localparam C3_DQ14_TAP_DELAY_VAL   = 0;       
   localparam C3_DQ15_TAP_DELAY_VAL   = 0;       
   localparam C3_MCB_USE_EXTERNAL_BUFPLL  = 1;       
   localparam C3_SMALL_DEVICE         = "FALSE";       // The parameter is set to TRUE for all packages of xc6slx9 device
                                                       // as most of them cannot fit the complete example design when the
                                                       // Chip scope modules are enabled
   localparam C3_INCLK_PERIOD         = ((C3_MEMCLK_PERIOD * C3_CLKFBOUT_MULT) / (C3_DIVCLK_DIVIDE * C3_CLKOUT0_DIVIDE * 2));       
   localparam DBG_WR_STS_WIDTH        = 32;
   localparam DBG_RD_STS_WIDTH        = 32;
   localparam C3_ARB_TIME0_SLOT  = {3'b000, 3'b000, 3'b000, 3'b000, 3'b000, C3_ARB_TIME_SLOT_0[2:0]};
   localparam C3_ARB_TIME1_SLOT  = {3'b000, 3'b000, 3'b000, 3'b000, 3'b000, C3_ARB_TIME_SLOT_1[2:0]};
   localparam C3_ARB_TIME2_SLOT  = {3'b000, 3'b000, 3'b000, 3'b000, 3'b000, C3_ARB_TIME_SLOT_2[2:0]};
   localparam C3_ARB_TIME3_SLOT  = {3'b000, 3'b000, 3'b000, 3'b000, 3'b000, C3_ARB_TIME_SLOT_3[2:0]};
   localparam C3_ARB_TIME4_SLOT  = {3'b000, 3'b000, 3'b000, 3'b000, 3'b000, C3_ARB_TIME_SLOT_4[2:0]};
   localparam C3_ARB_TIME5_SLOT  = {3'b000, 3'b000, 3'b000, 3'b000, 3'b000, C3_ARB_TIME_SLOT_5[2:0]};
   localparam C3_ARB_TIME6_SLOT  = {3'b000, 3'b000, 3'b000, 3'b000, 3'b000, C3_ARB_TIME_SLOT_6[2:0]};
   localparam C3_ARB_TIME7_SLOT  = {3'b000, 3'b000, 3'b000, 3'b000, 3'b000, C3_ARB_TIME_SLOT_7[2:0]};
   localparam C3_ARB_TIME8_SLOT  = {3'b000, 3'b000, 3'b000, 3'b000, 3'b000, C3_ARB_TIME_SLOT_8[2:0]};
   localparam C3_ARB_TIME9_SLOT  = {3'b000, 3'b000, 3'b000, 3'b000, 3'b000, C3_ARB_TIME_SLOT_9[2:0]};
   localparam C3_ARB_TIME10_SLOT  = {3'b000, 3'b000, 3'b000, 3'b000, 3'b000, C3_ARB_TIME_SLOT_10[2:0]};
   localparam C3_ARB_TIME11_SLOT  = {3'b000, 3'b000, 3'b000, 3'b000, 3'b000, C3_ARB_TIME_SLOT_11[2:0]};

  wire                              c3_sys_clk_p;
  wire                              c3_sys_clk_n;
  wire                              c3_async_rst;
  wire                              c3_sysclk_2x;
  wire                              c3_sysclk_2x_180;
  wire                              c3_pll_ce_0;
  wire                              c3_pll_ce_90;
  wire                              c3_pll_lock;
  wire                              c3_mcb_drp_clk;
  wire                              c3_cmp_error;
  wire                              c3_cmp_data_valid;
  wire                              c3_vio_modify_enable;
  wire  [2:0]                     c3_vio_data_mode_value;
  wire  [2:0]                     c3_vio_addr_mode_value;
  wire  [31:0]                     c3_cmp_data;
wire				c3_p1_cmd_clk;
wire				c3_p1_cmd_en;
wire[2:0]			c3_p1_cmd_instr;
wire[5:0]			c3_p1_cmd_bl;
wire[29:0]			c3_p1_cmd_byte_addr;
wire				c3_p1_cmd_empty;
wire				c3_p1_cmd_full;
wire				c3_p1_wr_clk;
wire				c3_p1_wr_en;
wire[C3_P1_MASK_SIZE-1:0]	c3_p1_wr_mask;
wire[C3_P1_DATA_PORT_SIZE-1:0]	c3_p1_wr_data;
wire				c3_p1_wr_full;
wire				c3_p1_wr_empty;
wire[6:0]			c3_p1_wr_count;
wire				c3_p1_wr_underrun;
wire				c3_p1_wr_error;
wire				c3_p1_rd_clk;
wire				c3_p1_rd_en;
wire[C3_P1_DATA_PORT_SIZE-1:0]	c3_p1_rd_data;
wire				c3_p1_rd_full;
wire				c3_p1_rd_empty;
wire[6:0]			c3_p1_rd_count;
wire				c3_p1_rd_overflow;
wire				c3_p1_rd_error;
wire				c3_p2_cmd_clk;
wire				c3_p2_cmd_en;
wire[2:0]			c3_p2_cmd_instr;
wire[5:0]			c3_p2_cmd_bl;
wire[29:0]			c3_p2_cmd_byte_addr;
wire				c3_p2_cmd_empty;
wire				c3_p2_cmd_full;
wire				c3_p2_wr_clk;
wire				c3_p2_wr_en;
wire[3:0]			c3_p2_wr_mask;
wire[31:0]			c3_p2_wr_data;
wire				c3_p2_wr_full;
wire				c3_p2_wr_empty;
wire[6:0]			c3_p2_wr_count;
wire				c3_p2_wr_underrun;
wire				c3_p2_wr_error;
wire				c3_p2_rd_clk;
wire				c3_p2_rd_en;
wire[31:0]			c3_p2_rd_data;
wire				c3_p2_rd_full;
wire				c3_p2_rd_empty;
wire[6:0]			c3_p2_rd_count;
wire				c3_p2_rd_overflow;
wire				c3_p2_rd_error;
wire				c3_p3_cmd_clk;
wire				c3_p3_cmd_en;
wire[2:0]			c3_p3_cmd_instr;
wire[5:0]			c3_p3_cmd_bl;
wire[29:0]			c3_p3_cmd_byte_addr;
wire				c3_p3_cmd_empty;
wire				c3_p3_cmd_full;
wire				c3_p3_wr_clk;
wire				c3_p3_wr_en;
wire[3:0]			c3_p3_wr_mask;
wire[31:0]			c3_p3_wr_data;
wire				c3_p3_wr_full;
wire				c3_p3_wr_empty;
wire[6:0]			c3_p3_wr_count;
wire				c3_p3_wr_underrun;
wire				c3_p3_wr_error;
wire				c3_p3_rd_clk;
wire				c3_p3_rd_en;
wire[31:0]			c3_p3_rd_data;
wire				c3_p3_rd_full;
wire				c3_p3_rd_empty;
wire[6:0]			c3_p3_rd_count;
wire				c3_p3_rd_overflow;
wire				c3_p3_rd_error;
wire				c3_p4_cmd_clk;
wire				c3_p4_cmd_en;
wire[2:0]			c3_p4_cmd_instr;
wire[5:0]			c3_p4_cmd_bl;
wire[29:0]			c3_p4_cmd_byte_addr;
wire				c3_p4_cmd_empty;
wire				c3_p4_cmd_full;
wire				c3_p4_wr_clk;
wire				c3_p4_wr_en;
wire[3:0]			c3_p4_wr_mask;
wire[31:0]			c3_p4_wr_data;
wire				c3_p4_wr_full;
wire				c3_p4_wr_empty;
wire[6:0]			c3_p4_wr_count;
wire				c3_p4_wr_underrun;
wire				c3_p4_wr_error;
wire				c3_p4_rd_clk;
wire				c3_p4_rd_en;
wire[31:0]			c3_p4_rd_data;
wire				c3_p4_rd_full;
wire				c3_p4_rd_empty;
wire[6:0]			c3_p4_rd_count;
wire				c3_p4_rd_overflow;
wire				c3_p4_rd_error;
wire				c3_p5_cmd_clk;
wire				c3_p5_cmd_en;
wire[2:0]			c3_p5_cmd_instr;
wire[5:0]			c3_p5_cmd_bl;
wire[29:0]			c3_p5_cmd_byte_addr;
wire				c3_p5_cmd_empty;
wire				c3_p5_cmd_full;
wire				c3_p5_wr_clk;
wire				c3_p5_wr_en;
wire[3:0]			c3_p5_wr_mask;
wire[31:0]			c3_p5_wr_data;
wire				c3_p5_wr_full;
wire				c3_p5_wr_empty;
wire[6:0]			c3_p5_wr_count;
wire				c3_p5_wr_underrun;
wire				c3_p5_wr_error;
wire				c3_p5_rd_clk;
wire				c3_p5_rd_en;
wire[31:0]			c3_p5_rd_data;
wire				c3_p5_rd_full;
wire				c3_p5_rd_empty;
wire[6:0]			c3_p5_rd_count;
wire				c3_p5_rd_overflow;
wire				c3_p5_rd_error;


   reg   c1_aresetn;
   reg   c3_aresetn;
   reg   c4_aresetn;
   reg   c5_aresetn;



assign  c3_sys_clk_p = 1'b0;
assign  c3_sys_clk_n = 1'b0;




// Infrastructure-3 instantiation
      infrastructure #
      (
         .C_INCLK_PERIOD                 (C3_INCLK_PERIOD),
         .C_RST_ACT_LOW                  (C3_RST_ACT_LOW),
         .C_INPUT_CLK_TYPE               (C3_INPUT_CLK_TYPE),
         .C_CLKOUT0_DIVIDE               (C3_CLKOUT0_DIVIDE),
         .C_CLKOUT1_DIVIDE               (C3_CLKOUT1_DIVIDE),
         .C_CLKOUT2_DIVIDE               (C3_CLKOUT2_DIVIDE),
         .C_CLKOUT3_DIVIDE               (C3_CLKOUT3_DIVIDE),
         .C_CLKFBOUT_MULT                (C3_CLKFBOUT_MULT),
         .C_DIVCLK_DIVIDE                (C3_DIVCLK_DIVIDE)
      )
      memc3_infrastructure_inst
      (
         .sys_clk_p                      (c3_sys_clk_p),  // [input] differential p type clock from board
         .sys_clk_n                      (c3_sys_clk_n),  // [input] differential n type clock from board
         .sys_clk                        (c3_sys_clk),    // [input] single ended input clock from board
         .sys_rst_i                      (c3_sys_rst_i),  
         .clk0                           (c3_clk0),       // [output] user clock which determines the operating frequency of user interface ports
         .rst0                           (c3_rst0),
         .async_rst                      (c3_async_rst),
         .sysclk_2x                      (c3_sysclk_2x),
         .sysclk_2x_180                  (c3_sysclk_2x_180),
         .pll_ce_0                       (c3_pll_ce_0),
         .pll_ce_90                      (c3_pll_ce_90),
         .pll_lock                       (c3_pll_lock),
         .mcb_drp_clk                    (c3_mcb_drp_clk)
      );
   


// Controller-3 instantiation
      memc_wrapper #
      (
         .C_MEMCLK_PERIOD                (C3_MEMCLK_PERIOD),   
         .C_CALIB_SOFT_IP                (C3_CALIB_SOFT_IP),
         //synthesis translate_off
         .C_SIMULATION                   (C3_SIMULATION),
         //synthesis translate_on
         .C_ARB_NUM_TIME_SLOTS           (C3_ARB_NUM_TIME_SLOTS),
         .C_ARB_TIME_SLOT_0              (C3_ARB_TIME0_SLOT),
         .C_ARB_TIME_SLOT_1              (C3_ARB_TIME1_SLOT),
         .C_ARB_TIME_SLOT_2              (C3_ARB_TIME2_SLOT),
         .C_ARB_TIME_SLOT_3              (C3_ARB_TIME3_SLOT),
         .C_ARB_TIME_SLOT_4              (C3_ARB_TIME4_SLOT),
         .C_ARB_TIME_SLOT_5              (C3_ARB_TIME5_SLOT),
         .C_ARB_TIME_SLOT_6              (C3_ARB_TIME6_SLOT),
         .C_ARB_TIME_SLOT_7              (C3_ARB_TIME7_SLOT),
         .C_ARB_TIME_SLOT_8              (C3_ARB_TIME8_SLOT),
         .C_ARB_TIME_SLOT_9              (C3_ARB_TIME9_SLOT),
         .C_ARB_TIME_SLOT_10             (C3_ARB_TIME10_SLOT),
         .C_ARB_TIME_SLOT_11             (C3_ARB_TIME11_SLOT),
         .C_ARB_ALGORITHM                (C3_ARB_ALGORITHM),
         .C_PORT_ENABLE                  (C3_PORT_ENABLE),
         .C_PORT_CONFIG                  (C3_PORT_CONFIG),
         .C_MEM_TRAS                     (C3_MEM_TRAS),
         .C_MEM_TRCD                     (C3_MEM_TRCD),
         .C_MEM_TREFI                    (C3_MEM_TREFI),
         .C_MEM_TRFC                     (C3_MEM_TRFC),
         .C_MEM_TRP                      (C3_MEM_TRP),
         .C_MEM_TWR                      (C3_MEM_TWR),
         .C_MEM_TRTP                     (C3_MEM_TRTP),
         .C_MEM_TWTR                     (C3_MEM_TWTR),
         .C_MEM_ADDR_ORDER               (C3_MEM_ADDR_ORDER),
         .C_NUM_DQ_PINS                  (C3_NUM_DQ_PINS),
         .C_MEM_TYPE                     (C3_MEM_TYPE),
         .C_MEM_DENSITY                  (C3_MEM_DENSITY),
         .C_MEM_BURST_LEN                (C3_MEM_BURST_LEN),
         .C_MEM_CAS_LATENCY              (C3_MEM_CAS_LATENCY),
         .C_MEM_ADDR_WIDTH               (C3_MEM_ADDR_WIDTH),
         .C_MEM_BANKADDR_WIDTH           (C3_MEM_BANKADDR_WIDTH),
         .C_MEM_NUM_COL_BITS             (C3_MEM_NUM_COL_BITS),
         .C_MEM_DDR1_2_ODS               (C3_MEM_DDR1_2_ODS),
         .C_MEM_DDR2_RTT                 (C3_MEM_DDR2_RTT),
         .C_MEM_DDR2_DIFF_DQS_EN         (C3_MEM_DDR2_DIFF_DQS_EN),
         .C_MEM_DDR2_3_PA_SR             (C3_MEM_DDR2_3_PA_SR),
         .C_MEM_DDR2_3_HIGH_TEMP_SR      (C3_MEM_DDR2_3_HIGH_TEMP_SR),
         .C_MEM_DDR3_CAS_LATENCY         (C3_MEM_DDR3_CAS_LATENCY),
         .C_MEM_DDR3_ODS                 (C3_MEM_DDR3_ODS),
         .C_MEM_DDR3_RTT                 (C3_MEM_DDR3_RTT),
         .C_MEM_DDR3_CAS_WR_LATENCY      (C3_MEM_DDR3_CAS_WR_LATENCY),
         .C_MEM_DDR3_AUTO_SR             (C3_MEM_DDR3_AUTO_SR),
         .C_MEM_MOBILE_PA_SR             (C3_MEM_MOBILE_PA_SR),
         .C_MEM_MDDR_ODS                 (C3_MEM_MDDR_ODS),
         .C_MC_CALIB_BYPASS              (C3_MC_CALIB_BYPASS),
         .C_MC_CALIBRATION_MODE          (C3_MC_CALIBRATION_MODE),
         .C_MC_CALIBRATION_DELAY         (C3_MC_CALIBRATION_DELAY),
         .C_SKIP_IN_TERM_CAL             (C3_SKIP_IN_TERM_CAL),
         .C_SKIP_DYNAMIC_CAL             (C3_SKIP_DYNAMIC_CAL),
         .LDQSP_TAP_DELAY_VAL            (C3_LDQSP_TAP_DELAY_VAL),
         .UDQSP_TAP_DELAY_VAL            (C3_UDQSP_TAP_DELAY_VAL),
         .LDQSN_TAP_DELAY_VAL            (C3_LDQSN_TAP_DELAY_VAL),
         .UDQSN_TAP_DELAY_VAL            (C3_UDQSN_TAP_DELAY_VAL),
         .DQ0_TAP_DELAY_VAL              (C3_DQ0_TAP_DELAY_VAL),
         .DQ1_TAP_DELAY_VAL              (C3_DQ1_TAP_DELAY_VAL),
         .DQ2_TAP_DELAY_VAL              (C3_DQ2_TAP_DELAY_VAL),
         .DQ3_TAP_DELAY_VAL              (C3_DQ3_TAP_DELAY_VAL),
         .DQ4_TAP_DELAY_VAL              (C3_DQ4_TAP_DELAY_VAL),
         .DQ5_TAP_DELAY_VAL              (C3_DQ5_TAP_DELAY_VAL),
         .DQ6_TAP_DELAY_VAL              (C3_DQ6_TAP_DELAY_VAL),
         .DQ7_TAP_DELAY_VAL              (C3_DQ7_TAP_DELAY_VAL),
         .DQ8_TAP_DELAY_VAL              (C3_DQ8_TAP_DELAY_VAL),
         .DQ9_TAP_DELAY_VAL              (C3_DQ9_TAP_DELAY_VAL),
         .DQ10_TAP_DELAY_VAL             (C3_DQ10_TAP_DELAY_VAL),
         .DQ11_TAP_DELAY_VAL             (C3_DQ11_TAP_DELAY_VAL),
         .DQ12_TAP_DELAY_VAL             (C3_DQ12_TAP_DELAY_VAL),
         .DQ13_TAP_DELAY_VAL             (C3_DQ13_TAP_DELAY_VAL),
         .DQ14_TAP_DELAY_VAL             (C3_DQ14_TAP_DELAY_VAL),
         .DQ15_TAP_DELAY_VAL             (C3_DQ15_TAP_DELAY_VAL),
         .C_P0_MASK_SIZE                 (C3_P0_MASK_SIZE),
         .C_P0_DATA_PORT_SIZE            (C3_P0_DATA_PORT_SIZE),
         .C_P1_MASK_SIZE                 (C3_P1_MASK_SIZE),
         .C_P1_DATA_PORT_SIZE            (C3_P1_DATA_PORT_SIZE)
	)
      
      memc3_wrapper_inst
      (
         .mcbx_dram_addr                 (mcb3_dram_a), 
         .mcbx_dram_ba                   (mcb3_dram_ba),
         .mcbx_dram_ras_n                (mcb3_dram_ras_n), 
         .mcbx_dram_cas_n                (mcb3_dram_cas_n), 
         .mcbx_dram_we_n                 (mcb3_dram_we_n), 
         .mcbx_dram_cke                  (mcb3_dram_cke), 
         .mcbx_dram_clk                  (mcb3_dram_ck), 
         .mcbx_dram_clk_n                (mcb3_dram_ck_n), 
         .mcbx_dram_dq                   (mcb3_dram_dq),
         .mcbx_dram_dqs                  (mcb3_dram_dqs), 
         .mcbx_dram_udqs                 (mcb3_dram_udqs), 
         .mcbx_dram_udm                  (mcb3_dram_udm), 
         .mcbx_dram_ldm                  (mcb3_dram_dm), 
         .mcbx_dram_dqs_n                ( ), 
         .mcbx_dram_udqs_n               ( ), 
         .mcbx_dram_odt                  ( ), 
         .mcbx_dram_ddr3_rst             ( ), 
         .mcbx_rzq                       (mcb3_rzq),
         .mcbx_zio                       ( ),
         .calib_done                     (c3_calib_done),
         .async_rst                      (c3_async_rst),
         .sysclk_2x                      (c3_sysclk_2x), 
         .sysclk_2x_180                  (c3_sysclk_2x_180), 
         .pll_ce_0                       (c3_pll_ce_0),
         .pll_ce_90                      (c3_pll_ce_90), 
         .pll_lock                       (c3_pll_lock),
         .mcb_drp_clk                    (c3_mcb_drp_clk), 
     
         // The following port map shows all the six logical user ports. However, all
	 // of them may not be active in this design. A port should be enabled to 
	 // validate its port map. If it is not,the complete port is going to float 
	 // by getting disconnected from the lower level MCB modules. The port enable
	 // information of a controller can be obtained from the corresponding local
	 // parameter CX_PORT_ENABLE. In such a case, we can simply ignore its port map.
	 // The following comments will explain when a port is going to be active.
	 // Config-1: Two 32-bit bi-directional and four 32-bit unidirectional ports
	 // Config-2: Four 32-bit bi-directional ports
	 // Config-3: One 64-bit bi-directional and two 32-bit bi-directional ports
	 // Config-4: Two 64-bit bi-directional ports
	 // Config-5: One 128-bit bi-directional port

         // User Port-0 command interface will be active only when the port is enabled in 
         // the port configurations Config-1, Config-2, Config-3, Config-4 and Config-5
         .p0_cmd_clk                     (c3_p0_cmd_clk), 
         .p0_cmd_en                      (c3_p0_cmd_en), 
         .p0_cmd_instr                   (c3_p0_cmd_instr),
         .p0_cmd_bl                      (c3_p0_cmd_bl), 
         .p0_cmd_byte_addr               (c3_p0_cmd_byte_addr), 
         .p0_cmd_full                    (c3_p0_cmd_full),
         .p0_cmd_empty                   (c3_p0_cmd_empty),
         // User Port-0 data write interface will be active only when the port is enabled in
         // the port configurations Config-1, Config-2, Config-3, Config-4 and Config-5
         .p0_wr_clk                      (c3_p0_wr_clk), 
         .p0_wr_en                       (c3_p0_wr_en),
         .p0_wr_mask                     (c3_p0_wr_mask),
         .p0_wr_data                     (c3_p0_wr_data),
         .p0_wr_full                     (c3_p0_wr_full),
         .p0_wr_count                    (c3_p0_wr_count),
         .p0_wr_empty                    (c3_p0_wr_empty),
         .p0_wr_underrun                 (c3_p0_wr_underrun),
         .p0_wr_error                    (c3_p0_wr_error),
         // User Port-0 data read interface will be active only when the port is enabled in
         // the port configurations Config-1, Config-2, Config-3, Config-4 and Config-5
         .p0_rd_clk                      (c3_p0_rd_clk), 
         .p0_rd_en                       (c3_p0_rd_en),
         .p0_rd_data                     (c3_p0_rd_data),
         .p0_rd_empty                    (c3_p0_rd_empty),
         .p0_rd_count                    (c3_p0_rd_count),
         .p0_rd_full                     (c3_p0_rd_full),
         .p0_rd_overflow                 (c3_p0_rd_overflow),
         .p0_rd_error                    (c3_p0_rd_error),
      
         // User Port-1 command interface will be active only when the port is enabled in 
         // the port configurations Config-1, Config-2, Config-3 and Config-4
         .p1_cmd_clk                     (c3_p1_cmd_clk), 
         .p1_cmd_en                      (c3_p1_cmd_en), 
         .p1_cmd_instr                   (c3_p1_cmd_instr),
         .p1_cmd_bl                      (c3_p1_cmd_bl), 
         .p1_cmd_byte_addr               (c3_p1_cmd_byte_addr), 
         .p1_cmd_full                    (c3_p1_cmd_full),
         .p1_cmd_empty                   (c3_p1_cmd_empty),
         // User Port-1 data write interface will be active only when the port is enabled in 
         // the port configurations Config-1, Config-2, Config-3 and Config-4
         .p1_wr_clk                      (c3_p1_wr_clk), 
         .p1_wr_en                       (c3_p1_wr_en),
         .p1_wr_mask                     (c3_p1_wr_mask),
         .p1_wr_data                     (c3_p1_wr_data),
         .p1_wr_full                     (c3_p1_wr_full),
         .p1_wr_count                    (c3_p1_wr_count),
         .p1_wr_empty                    (c3_p1_wr_empty),
         .p1_wr_underrun                 (c3_p1_wr_underrun),
         .p1_wr_error                    (c3_p1_wr_error),
         // User Port-1 data read interface will be active only when the port is enabled in 
         // the port configurations Config-1, Config-2, Config-3 and Config-4
         .p1_rd_clk                      (c3_p1_rd_clk), 
         .p1_rd_en                       (c3_p1_rd_en),
         .p1_rd_data                     (c3_p1_rd_data),
         .p1_rd_empty                    (c3_p1_rd_empty),
         .p1_rd_count                    (c3_p1_rd_count),
         .p1_rd_full                     (c3_p1_rd_full),
         .p1_rd_overflow                 (c3_p1_rd_overflow),
         .p1_rd_error                    (c3_p1_rd_error),
      
         // User Port-2 command interface will be active only when the port is enabled in 
         // the port configurations Config-1, Config-2 and Config-3
         .p2_cmd_clk                     (c3_p2_cmd_clk), 
         .p2_cmd_en                      (c3_p2_cmd_en), 
         .p2_cmd_instr                   (c3_p2_cmd_instr),
         .p2_cmd_bl                      (c3_p2_cmd_bl), 
         .p2_cmd_byte_addr               (c3_p2_cmd_byte_addr), 
         .p2_cmd_full                    (c3_p2_cmd_full),
         .p2_cmd_empty                   (c3_p2_cmd_empty),
         // User Port-2 data write interface will be active only when the port is enabled in 
         // the port configurations Config-1 write direction, Config-2 and Config-3
         .p2_wr_clk                      (c3_p2_wr_clk), 
         .p2_wr_en                       (c3_p2_wr_en),
         .p2_wr_mask                     (c3_p2_wr_mask),
         .p2_wr_data                     (c3_p2_wr_data),
         .p2_wr_full                     (c3_p2_wr_full),
         .p2_wr_count                    (c3_p2_wr_count),
         .p2_wr_empty                    (c3_p2_wr_empty),
         .p2_wr_underrun                 (c3_p2_wr_underrun),
         .p2_wr_error                    (c3_p2_wr_error),
         // User Port-2 data read interface will be active only when the port is enabled in 
         // the port configurations Config-1 read direction, Config-2 and Config-3
         .p2_rd_clk                      (c3_p2_rd_clk), 
         .p2_rd_en                       (c3_p2_rd_en),
         .p2_rd_data                     (c3_p2_rd_data),
         .p2_rd_empty                    (c3_p2_rd_empty),
         .p2_rd_count                    (c3_p2_rd_count),
         .p2_rd_full                     (c3_p2_rd_full),
         .p2_rd_overflow                 (c3_p2_rd_overflow),
         .p2_rd_error                    (c3_p2_rd_error),
      
         // User Port-3 command interface will be active only when the port is enabled in 
         // the port configurations Config-1 and Config-2
         .p3_cmd_clk                     (c3_p3_cmd_clk), 
         .p3_cmd_en                      (c3_p3_cmd_en), 
         .p3_cmd_instr                   (c3_p3_cmd_instr),
         .p3_cmd_bl                      (c3_p3_cmd_bl), 
         .p3_cmd_byte_addr               (c3_p3_cmd_byte_addr), 
         .p3_cmd_full                    (c3_p3_cmd_full),
         .p3_cmd_empty                   (c3_p3_cmd_empty),
         // User Port-3 data write interface will be active only when the port is enabled in 
         // the port configurations Config-1 write direction and Config-2
         .p3_wr_clk                      (c3_p3_wr_clk), 
         .p3_wr_en                       (c3_p3_wr_en),
         .p3_wr_mask                     (c3_p3_wr_mask),
         .p3_wr_data                     (c3_p3_wr_data),
         .p3_wr_full                     (c3_p3_wr_full),
         .p3_wr_count                    (c3_p3_wr_count),
         .p3_wr_empty                    (c3_p3_wr_empty),
         .p3_wr_underrun                 (c3_p3_wr_underrun),
         .p3_wr_error                    (c3_p3_wr_error),
         // User Port-3 data read interface will be active only when the port is enabled in 
         // the port configurations Config-1 read direction and Config-2
         .p3_rd_clk                      (c3_p3_rd_clk), 
         .p3_rd_en                       (c3_p3_rd_en),
         .p3_rd_data                     (c3_p3_rd_data),
         .p3_rd_empty                    (c3_p3_rd_empty),
         .p3_rd_count                    (c3_p3_rd_count),
         .p3_rd_full                     (c3_p3_rd_full),
         .p3_rd_overflow                 (c3_p3_rd_overflow),
         .p3_rd_error                    (c3_p3_rd_error),
      
         // User Port-4 command interface will be active only when the port is enabled in 
         // the port configuration Config-1
         .p4_cmd_clk                     (c3_p4_cmd_clk), 
         .p4_cmd_en                      (c3_p4_cmd_en), 
         .p4_cmd_instr                   (c3_p4_cmd_instr),
         .p4_cmd_bl                      (c3_p4_cmd_bl), 
         .p4_cmd_byte_addr               (c3_p4_cmd_byte_addr), 
         .p4_cmd_full                    (c3_p4_cmd_full),
         .p4_cmd_empty                   (c3_p4_cmd_empty),
         // User Port-4 data write interface will be active only when the port is enabled in 
         // the port configuration Config-1 write direction
         .p4_wr_clk                      (c3_p4_wr_clk), 
         .p4_wr_en                       (c3_p4_wr_en),
         .p4_wr_mask                     (c3_p4_wr_mask),
         .p4_wr_data                     (c3_p4_wr_data),
         .p4_wr_full                     (c3_p4_wr_full),
         .p4_wr_count                    (c3_p4_wr_count),
         .p4_wr_empty                    (c3_p4_wr_empty),
         .p4_wr_underrun                 (c3_p4_wr_underrun),
         .p4_wr_error                    (c3_p4_wr_error),
         // User Port-4 data read interface will be active only when the port is enabled in 
         // the port configuration Config-1 read direction
         .p4_rd_clk                      (c3_p4_rd_clk), 
         .p4_rd_en                       (c3_p4_rd_en),
         .p4_rd_data                     (c3_p4_rd_data),
         .p4_rd_empty                    (c3_p4_rd_empty),
         .p4_rd_count                    (c3_p4_rd_count),
         .p4_rd_full                     (c3_p4_rd_full),
         .p4_rd_overflow                 (c3_p4_rd_overflow),
         .p4_rd_error                    (c3_p4_rd_error),
      
         // User Port-5 command interface will be active only when the port is enabled in 
         // the port configuration Config-1
         .p5_cmd_clk                     (c3_p5_cmd_clk), 
         .p5_cmd_en                      (c3_p5_cmd_en), 
         .p5_cmd_instr                   (c3_p5_cmd_instr),
         .p5_cmd_bl                      (c3_p5_cmd_bl), 
         .p5_cmd_byte_addr               (c3_p5_cmd_byte_addr), 
         .p5_cmd_full                    (c3_p5_cmd_full),
         .p5_cmd_empty                   (c3_p5_cmd_empty),
         // User Port-5 data write interface will be active only when the port is enabled in 
         // the port configuration Config-1 write direction
         .p5_wr_clk                      (c3_p5_wr_clk), 
         .p5_wr_en                       (c3_p5_wr_en),
         .p5_wr_mask                     (c3_p5_wr_mask),
         .p5_wr_data                     (c3_p5_wr_data),
         .p5_wr_full                     (c3_p5_wr_full),
         .p5_wr_count                    (c3_p5_wr_count),
         .p5_wr_empty                    (c3_p5_wr_empty),
         .p5_wr_underrun                 (c3_p5_wr_underrun),
         .p5_wr_error                    (c3_p5_wr_error),
         // User Port-5 data read interface will be active only when the port is enabled in 
         // the port configuration Config-1 read direction
         .p5_rd_clk                      (c3_p5_rd_clk), 
         .p5_rd_en                       (c3_p5_rd_en),
         .p5_rd_data                     (c3_p5_rd_data),
         .p5_rd_empty                    (c3_p5_rd_empty),
         .p5_rd_count                    (c3_p5_rd_count),
         .p5_rd_full                     (c3_p5_rd_full),
         .p5_rd_overflow                 (c3_p5_rd_overflow),
         .p5_rd_error                    (c3_p5_rd_error),

         .selfrefresh_enter              (1'b0), 
         .selfrefresh_mode               (c3_selfrefresh_mode)
      );
   





endmodule   

module infrastructure #
  (
   parameter C_INCLK_PERIOD    = 2500,
   parameter C_RST_ACT_LOW      = 1,
   parameter C_INPUT_CLK_TYPE   = "DIFFERENTIAL",
   parameter C_CLKOUT0_DIVIDE   = 1,
   parameter C_CLKOUT1_DIVIDE   = 1,
   parameter C_CLKOUT2_DIVIDE   = 16,
   parameter C_CLKOUT3_DIVIDE   = 8,
   parameter C_CLKFBOUT_MULT    = 2,
   parameter C_DIVCLK_DIVIDE    = 1
   
   )
  (
   input  sys_clk_p,
   input  sys_clk_n,
   input  sys_clk,
   input  sys_rst_i,
   output clk0,
   output rst0,
   output async_rst,
   output sysclk_2x,
   output sysclk_2x_180,
   output mcb_drp_clk,
   output pll_ce_0,
   output pll_ce_90,
   output pll_lock

   );

  // # of clock cycles to delay deassertion of reset. Needs to be a fairly
  // high number not so much for metastability protection, but to give time
  // for reset (i.e. stable clock cycles) to propagate through all state
  // machines and to all control signals (i.e. not all control signals have
  // resets, instead they rely on base state logic being reset, and the effect
  // of that reset propagating through the logic). Need this because we may not
  // be getting stable clock cycles while reset asserted (i.e. since reset
  // depends on PLL/DCM lock status)

  localparam RST_SYNC_NUM = 25;
  localparam CLK_PERIOD_NS = C_INCLK_PERIOD / 1000.0;
  localparam CLK_PERIOD_INT = C_INCLK_PERIOD/1000;

  wire                       clk_2x_0;
  wire                       clk_2x_180;
  wire                       clk0_bufg;
  wire                       clk0_bufg_in;
  wire                       mcb_drp_clk_bufg_in;
  wire                       clkfbout_clkfbin;
  wire                       locked;
  reg [RST_SYNC_NUM-1:0]     rst0_sync_r    /* synthesis syn_maxfan = 10 */;
  wire                       rst_tmp;
  reg                        powerup_pll_locked;
  reg 			     syn_clk0_powerup_pll_locked;

  wire                       sys_rst;
  wire                       bufpll_mcb_locked;
  (* KEEP = "TRUE" *) wire sys_clk_ibufg;

  assign sys_rst = C_RST_ACT_LOW ? ~sys_rst_i: sys_rst_i;
  assign clk0        = clk0_bufg;
  assign pll_lock    = bufpll_mcb_locked;

  generate
    if (C_INPUT_CLK_TYPE == "DIFFERENTIAL") begin: diff_input_clk

      //***********************************************************************
      // Differential input clock input buffers
      //***********************************************************************

      IBUFGDS #
        (
         .DIFF_TERM    ("TRUE")
         )
        u_ibufg_sys_clk
          (
           .I  (sys_clk_p),
           .IB (sys_clk_n),
           .O  (sys_clk_ibufg)
           );

    end else if (C_INPUT_CLK_TYPE == "SINGLE_ENDED") begin: se_input_clk

      //***********************************************************************
      // SINGLE_ENDED input clock input buffers
      //***********************************************************************

      IBUFG  u_ibufg_sys_clk
          (
           .I  (sys_clk),
           .O  (sys_clk_ibufg)
           );
   end
  endgenerate

  //***************************************************************************
  // Global clock generation and distribution
  //***************************************************************************

    PLL_ADV #
        (
         .BANDWIDTH          ("OPTIMIZED"),
         .CLKIN1_PERIOD      (CLK_PERIOD_NS),
         .CLKIN2_PERIOD      (CLK_PERIOD_NS),
         .CLKOUT0_DIVIDE     (C_CLKOUT0_DIVIDE),
         .CLKOUT1_DIVIDE     (C_CLKOUT1_DIVIDE),
         .CLKOUT2_DIVIDE     (C_CLKOUT2_DIVIDE),
         .CLKOUT3_DIVIDE     (C_CLKOUT3_DIVIDE),
         .CLKOUT4_DIVIDE     (1),
         .CLKOUT5_DIVIDE     (1),
         .CLKOUT0_PHASE      (0.000),
         .CLKOUT1_PHASE      (180.000),
         .CLKOUT2_PHASE      (0.000),
         .CLKOUT3_PHASE      (0.000),
         .CLKOUT4_PHASE      (0.000),
         .CLKOUT5_PHASE      (0.000),
         .CLKOUT0_DUTY_CYCLE (0.500),
         .CLKOUT1_DUTY_CYCLE (0.500),
         .CLKOUT2_DUTY_CYCLE (0.500),
         .CLKOUT3_DUTY_CYCLE (0.500),
         .CLKOUT4_DUTY_CYCLE (0.500),
         .CLKOUT5_DUTY_CYCLE (0.500),
         .SIM_DEVICE         ("SPARTAN6"),
         .COMPENSATION       ("INTERNAL"),
         .DIVCLK_DIVIDE      (C_DIVCLK_DIVIDE),
         .CLKFBOUT_MULT      (C_CLKFBOUT_MULT),
         .CLKFBOUT_PHASE     (0.0),
         .REF_JITTER         (0.005000)
         )
        u_pll_adv
          (
           .CLKFBIN     (clkfbout_clkfbin),
           .CLKINSEL    (1'b1),
           .CLKIN1      (sys_clk_ibufg),
           .CLKIN2      (1'b0),
           .DADDR       (5'b0),
           .DCLK        (1'b0),
           .DEN         (1'b0),
           .DI          (16'b0),
           .DWE         (1'b0),
           .REL         (1'b0),
           .RST         (sys_rst),
           .CLKFBDCM    (),
           .CLKFBOUT    (clkfbout_clkfbin),
           .CLKOUTDCM0  (),
           .CLKOUTDCM1  (),
           .CLKOUTDCM2  (),
           .CLKOUTDCM3  (),
           .CLKOUTDCM4  (),
           .CLKOUTDCM5  (),
           .CLKOUT0     (clk_2x_0),
           .CLKOUT1     (clk_2x_180),
           .CLKOUT2     (clk0_bufg_in),
           .CLKOUT3     (mcb_drp_clk_bufg_in),
           .CLKOUT4     (),
           .CLKOUT5     (),
           .DO          (),
           .DRDY        (),
           .LOCKED      (locked)
           );

 

   BUFG U_BUFG_CLK0
    (
     .O (clk0_bufg),
     .I (clk0_bufg_in)
     );

   BUFGCE U_BUFG_CLK1
    (
     .O (mcb_drp_clk),
     .I (mcb_drp_clk_bufg_in),
     .CE (locked)
     );

  always @(posedge mcb_drp_clk , posedge sys_rst)
      if(sys_rst)
         powerup_pll_locked <= 1'b0;
       
      else if (bufpll_mcb_locked)
         powerup_pll_locked <= 1'b1;
         

  always @(posedge clk0_bufg , posedge sys_rst)
      if(sys_rst)
         syn_clk0_powerup_pll_locked <= 1'b0;
       
      else if (bufpll_mcb_locked)
         syn_clk0_powerup_pll_locked <= 1'b1;
         

  //***************************************************************************
  // Reset synchronization
  // NOTES:
  //   1. shut down the whole operation if the PLL hasn't yet locked (and
  //      by inference, this means that external SYS_RST_IN has been asserted -
  //      PLL deasserts LOCKED as soon as SYS_RST_IN asserted)
  //   2. asynchronously assert reset. This was we can assert reset even if
  //      there is no clock (needed for things like 3-stating output buffers).
  //      reset deassertion is synchronous.
  //   3. asynchronous reset only look at pll_lock from PLL during power up. After
  //      power up and pll_lock is asserted, the powerup_pll_locked will be asserted
  //      forever until sys_rst is asserted again. PLL will lose lock when FPGA 
  //      enters suspend mode. We don't want reset to MCB get
  //      asserted in the application that needs suspend feature.
  //***************************************************************************


  assign async_rst = sys_rst | ~powerup_pll_locked;
  // synthesis attribute max_fanout of rst0_sync_r is 10
  assign rst_tmp = sys_rst | ~syn_clk0_powerup_pll_locked;

  always @(posedge clk0_bufg or posedge rst_tmp)
    if (rst_tmp)
      rst0_sync_r <= {RST_SYNC_NUM{1'b1}};
    else
      // logical left shift by one (pads with 0)
      rst0_sync_r <= rst0_sync_r << 1;


  assign rst0    = rst0_sync_r[RST_SYNC_NUM-1];


BUFPLL_MCB BUFPLL_MCB1 
( .IOCLK0         (sysclk_2x),  
  .IOCLK1         (sysclk_2x_180),       
  .LOCKED         (locked),
  .GCLK           (mcb_drp_clk),
  .SERDESSTROBE0  (pll_ce_0), 
  .SERDESSTROBE1  (pll_ce_90), 
  .PLLIN0         (clk_2x_0),  
  .PLLIN1         (clk_2x_180),
  .LOCK           (bufpll_mcb_locked) 
  );


endmodule

module memc_wrapper  #
  (
   parameter         C_MEMCLK_PERIOD           = 2500,
   parameter         C_P0_MASK_SIZE            = 4,
   parameter         C_P0_DATA_PORT_SIZE       = 32,
   parameter         C_P1_MASK_SIZE            = 4,
   parameter         C_P1_DATA_PORT_SIZE       = 32,

   parameter         C_PORT_ENABLE             = 6'b111111,
   parameter         C_PORT_CONFIG             = "B128",
   parameter         C_MEM_ADDR_ORDER          = "BANK_ROW_COLUMN",
   // The following parameter reflects the GUI selection of the Arbitration algorithm.
   // Zero value corresponds to round robin algorithm and one to custom selection.
   // The parameter is used to calculate the arbitration time slot parameters.                           
   parameter         C_ARB_ALGORITHM           = 0,    								   					   
   parameter         C_ARB_NUM_TIME_SLOTS      = 12,
   parameter         C_ARB_TIME_SLOT_0         = 18'o012345,
   parameter         C_ARB_TIME_SLOT_1         = 18'o123450,
   parameter         C_ARB_TIME_SLOT_2         = 18'o234501,
   parameter         C_ARB_TIME_SLOT_3         = 18'o345012,
   parameter         C_ARB_TIME_SLOT_4         = 18'o450123,
   parameter         C_ARB_TIME_SLOT_5         = 18'o501234,
   parameter         C_ARB_TIME_SLOT_6         = 18'o012345,
   parameter         C_ARB_TIME_SLOT_7         = 18'o123450,
   parameter         C_ARB_TIME_SLOT_8         = 18'o234501,
   parameter         C_ARB_TIME_SLOT_9         = 18'o345012,
   parameter         C_ARB_TIME_SLOT_10        = 18'o450123,
   parameter         C_ARB_TIME_SLOT_11        = 18'o501234,
   parameter         C_MEM_TRAS                = 45000,
   parameter         C_MEM_TRCD                = 12500,
   parameter         C_MEM_TREFI               = 7800,
   parameter         C_MEM_TRFC                = 127500,
   parameter         C_MEM_TRP                 = 12500,
   parameter         C_MEM_TWR                 = 15000,
   parameter         C_MEM_TRTP                = 7500,
   parameter         C_MEM_TWTR                = 7500,
   parameter         C_NUM_DQ_PINS             = 8,
   parameter         C_MEM_TYPE                = "DDR3",
   parameter         C_MEM_DENSITY             = "512M",
   parameter         C_MEM_BURST_LEN           = 8,
   parameter         C_MEM_CAS_LATENCY         = 4,
   parameter         C_MEM_ADDR_WIDTH          = 13,
   parameter         C_MEM_BANKADDR_WIDTH      = 3,
   parameter         C_MEM_NUM_COL_BITS        = 11,
   parameter         C_MEM_DDR3_CAS_LATENCY    = 7,
   parameter         C_MEM_MOBILE_PA_SR        = "FULL",
   parameter         C_MEM_DDR1_2_ODS          = "FULL",
   parameter         C_MEM_DDR3_ODS            = "DIV6",
   parameter         C_MEM_DDR2_RTT            = "50OHMS",
   parameter         C_MEM_DDR3_RTT            = "DIV2",
   parameter         C_MEM_MDDR_ODS            = "FULL",
   parameter         C_MEM_DDR2_DIFF_DQS_EN    = "YES",
   parameter         C_MEM_DDR2_3_PA_SR        = "OFF",
   parameter         C_MEM_DDR3_CAS_WR_LATENCY = 5,
   parameter         C_MEM_DDR3_AUTO_SR        = "ENABLED",
   parameter         C_MEM_DDR2_3_HIGH_TEMP_SR = "NORMAL",
   parameter         C_MEM_DDR3_DYN_WRT_ODT    = "OFF",
   parameter         C_MC_CALIB_BYPASS         = "NO",

   parameter         LDQSP_TAP_DELAY_VAL       = 0,
   parameter         UDQSP_TAP_DELAY_VAL       = 0,
   parameter         LDQSN_TAP_DELAY_VAL       = 0,
   parameter         UDQSN_TAP_DELAY_VAL       = 0,
   parameter         DQ0_TAP_DELAY_VAL         = 0,
   parameter         DQ1_TAP_DELAY_VAL         = 0,
   parameter         DQ2_TAP_DELAY_VAL         = 0,
   parameter         DQ3_TAP_DELAY_VAL         = 0,
   parameter         DQ4_TAP_DELAY_VAL         = 0,
   parameter         DQ5_TAP_DELAY_VAL         = 0,
   parameter         DQ6_TAP_DELAY_VAL         = 0,
   parameter         DQ7_TAP_DELAY_VAL         = 0,
   parameter         DQ8_TAP_DELAY_VAL         = 0,
   parameter         DQ9_TAP_DELAY_VAL         = 0,
   parameter         DQ10_TAP_DELAY_VAL        = 0,
   parameter         DQ11_TAP_DELAY_VAL        = 0,
   parameter         DQ12_TAP_DELAY_VAL        = 0,
   parameter         DQ13_TAP_DELAY_VAL        = 0,
   parameter         DQ14_TAP_DELAY_VAL        = 0,
   parameter         DQ15_TAP_DELAY_VAL        = 0,

   parameter         C_CALIB_SOFT_IP           = "TRUE",
   parameter         C_SIMULATION              = "FALSE",
   parameter         C_SKIP_IN_TERM_CAL        = 1'b0,
   parameter         C_SKIP_DYNAMIC_CAL        = 1'b0,
   parameter         C_MC_CALIBRATION_MODE     = "CALIBRATION",
   parameter         C_MC_CALIBRATION_DELAY    = "HALF"

  )

  (

   // Raw Wrapper Signals
   input                                     sysclk_2x,          
   input                                     sysclk_2x_180, 
   input                                     pll_ce_0,
   input                                     pll_ce_90, 
   input                                     pll_lock,
   input                                     async_rst,
   input                                     mcb_drp_clk,       
   output      [C_MEM_ADDR_WIDTH-1:0]        mcbx_dram_addr,  
   output      [C_MEM_BANKADDR_WIDTH-1:0]    mcbx_dram_ba,
   output                                    mcbx_dram_ras_n,       
   output                                    mcbx_dram_cas_n,       
   output                                    mcbx_dram_we_n,  
   output                                    mcbx_dram_cke, 
   output                                    mcbx_dram_clk, 
   output                                    mcbx_dram_clk_n,       
   inout       [C_NUM_DQ_PINS-1:0]           mcbx_dram_dq,
   inout                                     mcbx_dram_dqs, 
   inout                                     mcbx_dram_dqs_n,       
   inout                                     mcbx_dram_udqs,  
   inout                                     mcbx_dram_udqs_n,       
   output                                    mcbx_dram_udm, 
   output                                    mcbx_dram_ldm, 
   output                                    mcbx_dram_odt, 
   output                                    mcbx_dram_ddr3_rst,      
   inout                                     mcbx_rzq,
   inout                                     mcbx_zio,
   output                                    calib_done,
   input                                     selfrefresh_enter,       
   output                                    selfrefresh_mode,

// This new memc_wrapper shows all the six logical static user ports. The port
// configuration parameter and the port enable parameter are the ones that 
// determine the active and non-active ports. The following list shows the 
// default active ports for each port configuration.
//
// Config 1: "B32_B32_X32_X32_X32_X32"
//            User port 0  --> 32 bit,  User port 1  --> 32 bit 
//            User port 2  --> 32 bit,  User port 3  --> 32 bit
//            User port 4  --> 32 bit,  User port 5  --> 32 bit
// Config 2: "B32_B32_B32_B32"  
//            User port 0  --> 32 bit 
//            User port 1  --> 32 bit 
//            User port 2  --> 32 bit 
//            User port 3  --> 32 bit 
// Config 3: "B64_B32_B3"  
//            User port 0  --> 64 bit 
//            User port 1  --> 32 bit 
//            User port 2  --> 32 bit 
// Config 4: "B64_B64"          
//            User port 0  --> 64 bit 
//            User port 1  --> 64 bit
// Config 5  "B128"          
//            User port 0  --> 128 bit


   // User Port-0 command interface will be active only when the port is enabled in 
   // the port configurations Config-1, Config-2, Config-3, Config-4 and Config-5
   input                                     p0_cmd_clk, 
   input                                     p0_cmd_en, 
   input       [2:0]                         p0_cmd_instr,
   input       [5:0]                         p0_cmd_bl, 
   input       [29:0]                        p0_cmd_byte_addr,       
   output                                    p0_cmd_full,
   output                                    p0_cmd_empty,
   // User Port-0 data write interface will be active only when the port is enabled in
   // the port configurations Config-1, Config-2, Config-3, Config-4 and Config-5
   input                                     p0_wr_clk,       
   input                                     p0_wr_en,
   input       [C_P0_MASK_SIZE-1:0]          p0_wr_mask,
   input       [C_P0_DATA_PORT_SIZE-1:0]     p0_wr_data,
   output                                    p0_wr_full,
   output      [6:0]                         p0_wr_count,
   output                                    p0_wr_empty,
   output                                    p0_wr_underrun,  
   output                                    p0_wr_error,
   // User Port-0 data read interface will be active only when the port is enabled in
   // the port configurations Config-1, Config-2, Config-3, Config-4 and Config-5
   input                                     p0_rd_clk,
   input                                     p0_rd_en,
   output      [C_P0_DATA_PORT_SIZE-1:0]     p0_rd_data,
   output                                    p0_rd_empty,
   output      [6:0]                         p0_rd_count,
   output                                    p0_rd_full,
   output                                    p0_rd_overflow,  
   output                                    p0_rd_error,

   // User Port-1 command interface will be active only when the port is enabled in 
   // the port configurations Config-1, Config-2, Config-3 and Config-4
   input                                     p1_cmd_clk, 
   input                                     p1_cmd_en, 
   input       [2:0]                         p1_cmd_instr,
   input       [5:0]                         p1_cmd_bl, 
   input       [29:0]                        p1_cmd_byte_addr,       
   output                                    p1_cmd_full,
   output                                    p1_cmd_empty,
   // User Port-1 data write interface will be active only when the port is enabled in 
   // the port configurations Config-1, Config-2, Config-3 and Config-4
   input                                     p1_wr_clk,       
   input                                     p1_wr_en,
   input       [C_P1_MASK_SIZE-1:0]          p1_wr_mask,
   input       [C_P1_DATA_PORT_SIZE-1:0]     p1_wr_data,
   output                                    p1_wr_full,
   output      [6:0]                         p1_wr_count,
   output                                    p1_wr_empty,
   output                                    p1_wr_underrun,  
   output                                    p1_wr_error,
   // User Port-1 data read interface will be active only when the port is enabled in 
   // the port configurations Config-1, Config-2, Config-3 and Config-4
   input                                     p1_rd_clk,
   input                                     p1_rd_en,
   output      [C_P1_DATA_PORT_SIZE-1:0]     p1_rd_data,
   output                                    p1_rd_empty,
   output      [6:0]                         p1_rd_count,
   output                                    p1_rd_full,
   output                                    p1_rd_overflow,  
   output                                    p1_rd_error,

   // User Port-2 command interface will be active only when the port is enabled in 
   // the port configurations Config-1, Config-2 and Config-3
   input                                     p2_cmd_clk, 
   input                                     p2_cmd_en, 
   input       [2:0]                         p2_cmd_instr,
   input       [5:0]                         p2_cmd_bl, 
   input       [29:0]                        p2_cmd_byte_addr,       
   output                                    p2_cmd_full,
   output                                    p2_cmd_empty,
   // User Port-2 data write interface will be active only when the port is enabled in 
   // the port configurations Config-1 write direction, Config-2 and Config-3
   input                                     p2_wr_clk,       
   input                                     p2_wr_en,
   input       [3:0]                         p2_wr_mask,
   input       [31:0]                        p2_wr_data,
   output                                    p2_wr_full,
   output      [6:0]                         p2_wr_count,
   output                                    p2_wr_empty,
   output                                    p2_wr_underrun,  
   output                                    p2_wr_error,
   // User Port-2 data read interface will be active only when the port is enabled in 
   // the port configurations Config-1 read direction, Config-2 and Config-3
   input                                     p2_rd_clk,
   input                                     p2_rd_en,
   output      [31:0]                        p2_rd_data,
   output                                    p2_rd_empty,
   output      [6:0]                         p2_rd_count,
   output                                    p2_rd_full,
   output                                    p2_rd_overflow,  
   output                                    p2_rd_error,

   // User Port-3 command interface will be active only when the port is enabled in 
   // the port configurations Config-1 and Config-2
   input                                     p3_cmd_clk, 
   input                                     p3_cmd_en, 
   input       [2:0]                         p3_cmd_instr,
   input       [5:0]                         p3_cmd_bl, 
   input       [29:0]                        p3_cmd_byte_addr,       
   output                                    p3_cmd_full,
   output                                    p3_cmd_empty,
   // User Port-3 data write interface will be active only when the port is enabled in 
   // the port configurations Config-1 write direction and Config-2
   input                                     p3_wr_clk,       
   input                                     p3_wr_en,
   input       [3:0]                         p3_wr_mask,
   input       [31:0]                        p3_wr_data,
   output                                    p3_wr_full,
   output      [6:0]                         p3_wr_count,
   output                                    p3_wr_empty,
   output                                    p3_wr_underrun,  
   output                                    p3_wr_error,
   // User Port-3 data read interface will be active only when the port is enabled in 
   // the port configurations Config-1 read direction and Config-2
   input                                     p3_rd_clk,
   input                                     p3_rd_en,
   output      [31:0]                        p3_rd_data,
   output                                    p3_rd_empty,
   output      [6:0]                         p3_rd_count,
   output                                    p3_rd_full,
   output                                    p3_rd_overflow,  
   output                                    p3_rd_error,

   // User Port-4 command interface will be active only when the port is enabled in 
   // the port configuration Config-1
   input                                     p4_cmd_clk, 
   input                                     p4_cmd_en, 
   input       [2:0]                         p4_cmd_instr,
   input       [5:0]                         p4_cmd_bl, 
   input       [29:0]                        p4_cmd_byte_addr,       
   output                                    p4_cmd_full,
   output                                    p4_cmd_empty,
   // User Port-4 data write interface will be active only when the port is enabled in 
   // the port configuration Config-1 write direction
   input                                     p4_wr_clk,       
   input                                     p4_wr_en,
   input       [3:0]                         p4_wr_mask,
   input       [31:0]                        p4_wr_data,
   output                                    p4_wr_full,
   output      [6:0]                         p4_wr_count,
   output                                    p4_wr_empty,
   output                                    p4_wr_underrun,  
   output                                    p4_wr_error,
   // User Port-4 data read interface will be active only when the port is enabled in 
   // the port configuration Config-1 read direction
   input                                     p4_rd_clk,
   input                                     p4_rd_en,
   output      [31:0]                        p4_rd_data,
   output                                    p4_rd_empty,
   output      [6:0]                         p4_rd_count,
   output                                    p4_rd_full,
   output                                    p4_rd_overflow,  
   output                                    p4_rd_error,
   // User Port-5 command interface will be active only when the port is enabled in 
   // the port configuration Config-1
   input                                     p5_cmd_clk, 
   input                                     p5_cmd_en, 
   input       [2:0]                         p5_cmd_instr,
   input       [5:0]                         p5_cmd_bl, 
   input       [29:0]                        p5_cmd_byte_addr,       
   output                                    p5_cmd_full,
   output                                    p5_cmd_empty,
   // User Port-5 data write interface will be active only when the port is enabled in 
   // the port configuration Config-1 write direction
   input                                     p5_wr_clk,       
   input                                     p5_wr_en,
   input       [3:0]                         p5_wr_mask,
   input       [31:0]                        p5_wr_data,
   output                                    p5_wr_full,
   output      [6:0]                         p5_wr_count,
   output                                    p5_wr_empty,
   output                                    p5_wr_underrun,  
   output                                    p5_wr_error,
   // User Port-5 data read interface will be active only when the port is enabled in 
   // the port configuration Config-1 read direction
   input                                     p5_rd_clk,
   input                                     p5_rd_en,
   output      [31:0]                        p5_rd_data,
   output                                    p5_rd_empty,
   output      [6:0]                         p5_rd_count,
   output                                    p5_rd_full,
   output                                    p5_rd_overflow,  
   output                                    p5_rd_error

  );
  
   localparam C_MC_CALIBRATION_CLK_DIV  = 1;
   localparam C_MEM_TZQINIT_MAXCNT      = 10'd512 + 10'd16;   // 16 clock cycles are added to avoid trfc violations
   localparam C_SKIP_DYN_IN_TERM        = 1'b1;

   localparam C_MC_CALIBRATION_RA       = 16'h0000;       
   localparam C_MC_CALIBRATION_BA       = 3'h0;       
   localparam C_MC_CALIBRATION_CA       = 12'h000;       

// All the following new localparams and signals are added to support 
// the AXI slave interface. They have no function to play in a standard
// interface design and can be ignored. 
   localparam C_S0_AXI_ID_WIDTH         = 4;
   localparam C_S0_AXI_ADDR_WIDTH       = 64;
   localparam C_S0_AXI_DATA_WIDTH       = 32;
   localparam C_S1_AXI_ID_WIDTH         = 4;
   localparam C_S1_AXI_ADDR_WIDTH       = 64;
   localparam C_S1_AXI_DATA_WIDTH       = 32;
   localparam C_S2_AXI_ID_WIDTH         = 4;
   localparam C_S2_AXI_ADDR_WIDTH       = 64;
   localparam C_S2_AXI_DATA_WIDTH       = 32;
   localparam C_S3_AXI_ID_WIDTH         = 4;
   localparam C_S3_AXI_ADDR_WIDTH       = 64;
   localparam C_S3_AXI_DATA_WIDTH       = 32;
   localparam C_S4_AXI_ID_WIDTH         = 4;
   localparam C_S4_AXI_ADDR_WIDTH       = 64;
   localparam C_S4_AXI_DATA_WIDTH       = 32;
   localparam C_S5_AXI_ID_WIDTH         = 4;
   localparam C_S5_AXI_ADDR_WIDTH       = 64;
   localparam C_S5_AXI_DATA_WIDTH       = 32;
   localparam C_MCB_USE_EXTERNAL_BUFPLL = 1;

// AXI wire declarations
// AXI interface of the mcb_ui_top module is connected to the following
// floating wires in all the standard interface designs.
   wire                                      s0_axi_aclk;
   wire                                      s0_axi_aresetn;
   wire [C_S0_AXI_ID_WIDTH-1:0]              s0_axi_awid; 
   wire [C_S0_AXI_ADDR_WIDTH-1:0]            s0_axi_awaddr; 
   wire [7:0]                                s0_axi_awlen; 
   wire [2:0]                                s0_axi_awsize; 
   wire [1:0]                                s0_axi_awburst; 
   wire [0:0]                                s0_axi_awlock; 
   wire [3:0]                                s0_axi_awcache; 
   wire [2:0]                                s0_axi_awprot; 
   wire [3:0]                                s0_axi_awqos; 
   wire                                      s0_axi_awvalid; 
   wire                                      s0_axi_awready; 
   wire [C_S0_AXI_DATA_WIDTH-1:0]            s0_axi_wdata; 
   wire [C_S0_AXI_DATA_WIDTH/8-1:0]          s0_axi_wstrb; 
   wire                                      s0_axi_wlast; 
   wire                                      s0_axi_wvalid; 
   wire                                      s0_axi_wready; 
   wire [C_S0_AXI_ID_WIDTH-1:0]              s0_axi_bid; 
   wire [1:0]                                s0_axi_bresp; 
   wire                                      s0_axi_bvalid; 
   wire                                      s0_axi_bready; 
   wire [C_S0_AXI_ID_WIDTH-1:0]              s0_axi_arid; 
   wire [C_S0_AXI_ADDR_WIDTH-1:0]            s0_axi_araddr; 
   wire [7:0]                                s0_axi_arlen; 
   wire [2:0]                                s0_axi_arsize; 
   wire [1:0]                                s0_axi_arburst; 
   wire [0:0]                                s0_axi_arlock; 
   wire [3:0]                                s0_axi_arcache; 
   wire [2:0]                                s0_axi_arprot; 
   wire [3:0]                                s0_axi_arqos; 
   wire                                      s0_axi_arvalid; 
   wire                                      s0_axi_arready; 
   wire [C_S0_AXI_ID_WIDTH-1:0]              s0_axi_rid; 
   wire [C_S0_AXI_DATA_WIDTH-1:0]            s0_axi_rdata; 
   wire [1:0]                                s0_axi_rresp; 
   wire                                      s0_axi_rlast; 
   wire                                      s0_axi_rvalid; 
   wire                                      s0_axi_rready;

   wire                                      s1_axi_aclk;
   wire                                      s1_axi_aresetn;
   wire [C_S1_AXI_ID_WIDTH-1:0]              s1_axi_awid; 
   wire [C_S1_AXI_ADDR_WIDTH-1:0]            s1_axi_awaddr; 
   wire [7:0]                                s1_axi_awlen; 
   wire [2:0]                                s1_axi_awsize; 
   wire [1:0]                                s1_axi_awburst; 
   wire [0:0]                                s1_axi_awlock; 
   wire [3:0]                                s1_axi_awcache; 
   wire [2:0]                                s1_axi_awprot; 
   wire [3:0]                                s1_axi_awqos; 
   wire                                      s1_axi_awvalid; 
   wire                                      s1_axi_awready; 
   wire [C_S1_AXI_DATA_WIDTH-1:0]            s1_axi_wdata; 
   wire [C_S1_AXI_DATA_WIDTH/8-1:0]          s1_axi_wstrb; 
   wire                                      s1_axi_wlast; 
   wire                                      s1_axi_wvalid; 
   wire                                      s1_axi_wready; 
   wire [C_S1_AXI_ID_WIDTH-1:0]              s1_axi_bid; 
   wire [1:0]                                s1_axi_bresp; 
   wire                                      s1_axi_bvalid; 
   wire                                      s1_axi_bready; 
   wire [C_S1_AXI_ID_WIDTH-1:0]              s1_axi_arid; 
   wire [C_S1_AXI_ADDR_WIDTH-1:0]            s1_axi_araddr; 
   wire [7:0]                                s1_axi_arlen; 
   wire [2:0]                                s1_axi_arsize; 
   wire [1:0]                                s1_axi_arburst; 
   wire [0:0]                                s1_axi_arlock; 
   wire [3:0]                                s1_axi_arcache; 
   wire [2:0]                                s1_axi_arprot; 
   wire [3:0]                                s1_axi_arqos; 
   wire                                      s1_axi_arvalid; 
   wire                                      s1_axi_arready; 
   wire [C_S1_AXI_ID_WIDTH-1:0]              s1_axi_rid; 
   wire [C_S1_AXI_DATA_WIDTH-1:0]            s1_axi_rdata; 
   wire [1:0]                                s1_axi_rresp; 
   wire                                      s1_axi_rlast; 
   wire                                      s1_axi_rvalid; 
   wire                                      s1_axi_rready;

   wire                                      s2_axi_aclk;
   wire                                      s2_axi_aresetn;
   wire [C_S2_AXI_ID_WIDTH-1:0]              s2_axi_awid; 
   wire [C_S2_AXI_ADDR_WIDTH-1:0]            s2_axi_awaddr; 
   wire [7:0]                                s2_axi_awlen; 
   wire [2:0]                                s2_axi_awsize; 
   wire [1:0]                                s2_axi_awburst; 
   wire [0:0]                                s2_axi_awlock; 
   wire [3:0]                                s2_axi_awcache; 
   wire [2:0]                                s2_axi_awprot; 
   wire [3:0]                                s2_axi_awqos; 
   wire                                      s2_axi_awvalid; 
   wire                                      s2_axi_awready; 
   wire [C_S2_AXI_DATA_WIDTH-1:0]            s2_axi_wdata; 
   wire [C_S2_AXI_DATA_WIDTH/8-1:0]          s2_axi_wstrb; 
   wire                                      s2_axi_wlast; 
   wire                                      s2_axi_wvalid; 
   wire                                      s2_axi_wready; 
   wire [C_S2_AXI_ID_WIDTH-1:0]              s2_axi_bid; 
   wire [1:0]                                s2_axi_bresp; 
   wire                                      s2_axi_bvalid; 
   wire                                      s2_axi_bready; 
   wire [C_S2_AXI_ID_WIDTH-1:0]              s2_axi_arid; 
   wire [C_S2_AXI_ADDR_WIDTH-1:0]            s2_axi_araddr; 
   wire [7:0]                                s2_axi_arlen; 
   wire [2:0]                                s2_axi_arsize; 
   wire [1:0]                                s2_axi_arburst; 
   wire [0:0]                                s2_axi_arlock; 
   wire [3:0]                                s2_axi_arcache; 
   wire [2:0]                                s2_axi_arprot; 
   wire [3:0]                                s2_axi_arqos; 
   wire                                      s2_axi_arvalid; 
   wire                                      s2_axi_arready; 
   wire [C_S2_AXI_ID_WIDTH-1:0]              s2_axi_rid; 
   wire [C_S2_AXI_DATA_WIDTH-1:0]            s2_axi_rdata; 
   wire [1:0]                                s2_axi_rresp; 
   wire                                      s2_axi_rlast; 
   wire                                      s2_axi_rvalid; 
   wire                                      s2_axi_rready;

   wire                                      s3_axi_aclk;
   wire                                      s3_axi_aresetn;
   wire [C_S3_AXI_ID_WIDTH-1:0]              s3_axi_awid; 
   wire [C_S3_AXI_ADDR_WIDTH-1:0]            s3_axi_awaddr; 
   wire [7:0]                                s3_axi_awlen; 
   wire [2:0]                                s3_axi_awsize; 
   wire [1:0]                                s3_axi_awburst; 
   wire [0:0]                                s3_axi_awlock; 
   wire [3:0]                                s3_axi_awcache; 
   wire [2:0]                                s3_axi_awprot; 
   wire [3:0]                                s3_axi_awqos; 
   wire                                      s3_axi_awvalid; 
   wire                                      s3_axi_awready; 
   wire [C_S3_AXI_DATA_WIDTH-1:0]            s3_axi_wdata; 
   wire [C_S3_AXI_DATA_WIDTH/8-1:0]          s3_axi_wstrb; 
   wire                                      s3_axi_wlast; 
   wire                                      s3_axi_wvalid; 
   wire                                      s3_axi_wready; 
   wire [C_S3_AXI_ID_WIDTH-1:0]              s3_axi_bid; 
   wire [1:0]                                s3_axi_bresp; 
   wire                                      s3_axi_bvalid; 
   wire                                      s3_axi_bready; 
   wire [C_S3_AXI_ID_WIDTH-1:0]              s3_axi_arid; 
   wire [C_S3_AXI_ADDR_WIDTH-1:0]            s3_axi_araddr; 
   wire [7:0]                                s3_axi_arlen; 
   wire [2:0]                                s3_axi_arsize; 
   wire [1:0]                                s3_axi_arburst; 
   wire [0:0]                                s3_axi_arlock; 
   wire [3:0]                                s3_axi_arcache; 
   wire [2:0]                                s3_axi_arprot; 
   wire [3:0]                                s3_axi_arqos; 
   wire                                      s3_axi_arvalid; 
   wire                                      s3_axi_arready; 
   wire [C_S3_AXI_ID_WIDTH-1:0]              s3_axi_rid; 
   wire [C_S3_AXI_DATA_WIDTH-1:0]            s3_axi_rdata; 
   wire [1:0]                                s3_axi_rresp; 
   wire                                      s3_axi_rlast; 
   wire                                      s3_axi_rvalid; 
   wire                                      s3_axi_rready;

   wire                                      s4_axi_aclk;
   wire                                      s4_axi_aresetn;
   wire [C_S4_AXI_ID_WIDTH-1:0]              s4_axi_awid; 
   wire [C_S4_AXI_ADDR_WIDTH-1:0]            s4_axi_awaddr; 
   wire [7:0]                                s4_axi_awlen; 
   wire [2:0]                                s4_axi_awsize; 
   wire [1:0]                                s4_axi_awburst; 
   wire [0:0]                                s4_axi_awlock; 
   wire [3:0]                                s4_axi_awcache; 
   wire [2:0]                                s4_axi_awprot; 
   wire [3:0]                                s4_axi_awqos; 
   wire                                      s4_axi_awvalid; 
   wire                                      s4_axi_awready; 
   wire [C_S4_AXI_DATA_WIDTH-1:0]            s4_axi_wdata; 
   wire [C_S4_AXI_DATA_WIDTH/8-1:0]          s4_axi_wstrb; 
   wire                                      s4_axi_wlast; 
   wire                                      s4_axi_wvalid; 
   wire                                      s4_axi_wready; 
   wire [C_S4_AXI_ID_WIDTH-1:0]              s4_axi_bid; 
   wire [1:0]                                s4_axi_bresp; 
   wire                                      s4_axi_bvalid; 
   wire                                      s4_axi_bready; 
   wire [C_S4_AXI_ID_WIDTH-1:0]              s4_axi_arid; 
   wire [C_S4_AXI_ADDR_WIDTH-1:0]            s4_axi_araddr; 
   wire [7:0]                                s4_axi_arlen; 
   wire [2:0]                                s4_axi_arsize; 
   wire [1:0]                                s4_axi_arburst; 
   wire [0:0]                                s4_axi_arlock; 
   wire [3:0]                                s4_axi_arcache; 
   wire [2:0]                                s4_axi_arprot; 
   wire [3:0]                                s4_axi_arqos; 
   wire                                      s4_axi_arvalid; 
   wire                                      s4_axi_arready; 
   wire [C_S4_AXI_ID_WIDTH-1:0]              s4_axi_rid; 
   wire [C_S4_AXI_DATA_WIDTH-1:0]            s4_axi_rdata; 
   wire [1:0]                                s4_axi_rresp; 
   wire                                      s4_axi_rlast; 
   wire                                      s4_axi_rvalid; 
   wire                                      s4_axi_rready;

   wire                                      s5_axi_aclk;
   wire                                      s5_axi_aresetn;
   wire [C_S5_AXI_ID_WIDTH-1:0]              s5_axi_awid; 
   wire [C_S5_AXI_ADDR_WIDTH-1:0]            s5_axi_awaddr; 
   wire [7:0]                                s5_axi_awlen; 
   wire [2:0]                                s5_axi_awsize; 
   wire [1:0]                                s5_axi_awburst; 
   wire [0:0]                                s5_axi_awlock; 
   wire [3:0]                                s5_axi_awcache; 
   wire [2:0]                                s5_axi_awprot; 
   wire [3:0]                                s5_axi_awqos; 
   wire                                      s5_axi_awvalid; 
   wire                                      s5_axi_awready; 
   wire [C_S5_AXI_DATA_WIDTH-1:0]            s5_axi_wdata; 
   wire [C_S5_AXI_DATA_WIDTH/8-1:0]          s5_axi_wstrb; 
   wire                                      s5_axi_wlast; 
   wire                                      s5_axi_wvalid; 
   wire                                      s5_axi_wready; 
   wire [C_S5_AXI_ID_WIDTH-1:0]              s5_axi_bid; 
   wire [1:0]                                s5_axi_bresp; 
   wire                                      s5_axi_bvalid; 
   wire                                      s5_axi_bready; 
   wire [C_S5_AXI_ID_WIDTH-1:0]              s5_axi_arid; 
   wire [C_S5_AXI_ADDR_WIDTH-1:0]            s5_axi_araddr; 
   wire [7:0]                                s5_axi_arlen; 
   wire [2:0]                                s5_axi_arsize; 
   wire [1:0]                                s5_axi_arburst; 
   wire [0:0]                                s5_axi_arlock; 
   wire [3:0]                                s5_axi_arcache; 
   wire [2:0]                                s5_axi_arprot; 
   wire [3:0]                                s5_axi_arqos; 
   wire                                      s5_axi_arvalid; 
   wire                                      s5_axi_arready; 
   wire [C_S5_AXI_ID_WIDTH-1:0]              s5_axi_rid; 
   wire [C_S5_AXI_DATA_WIDTH-1:0]            s5_axi_rdata; 
   wire [1:0]                                s5_axi_rresp; 
   wire                                      s5_axi_rlast; 
   wire                                      s5_axi_rvalid; 
   wire                                      s5_axi_rready;

   wire [7:0]                                uo_data;        
   wire                                      uo_data_valid;  
   wire                                      uo_cmd_ready_in;
   wire                                      uo_refrsh_flag; 
   wire                                      uo_cal_start;   
   wire                                      uo_sdo;
   wire [31:0]                               status;  
   wire                                      sysclk_2x_bufpll_o;
   wire                                      sysclk_2x_180_bufpll_o;
   wire                                      pll_ce_0_bufpll_o;
   wire                                      pll_ce_90_bufpll_o;
   wire                                      pll_lock_bufpll_o;


// mcb_ui_top instantiation
mcb_ui_top #
  (
   // Raw Wrapper Parameters
   .C_MEMCLK_PERIOD               (C_MEMCLK_PERIOD), 
   .C_PORT_ENABLE                 (C_PORT_ENABLE), 
   .C_MEM_ADDR_ORDER              (C_MEM_ADDR_ORDER), 
   .C_ARB_ALGORITHM               (C_ARB_ALGORITHM), 
   .C_ARB_NUM_TIME_SLOTS          (C_ARB_NUM_TIME_SLOTS), 
   .C_ARB_TIME_SLOT_0             (C_ARB_TIME_SLOT_0), 
   .C_ARB_TIME_SLOT_1             (C_ARB_TIME_SLOT_1), 
   .C_ARB_TIME_SLOT_2             (C_ARB_TIME_SLOT_2), 
   .C_ARB_TIME_SLOT_3             (C_ARB_TIME_SLOT_3), 
   .C_ARB_TIME_SLOT_4             (C_ARB_TIME_SLOT_4), 
   .C_ARB_TIME_SLOT_5             (C_ARB_TIME_SLOT_5), 
   .C_ARB_TIME_SLOT_6             (C_ARB_TIME_SLOT_6), 
   .C_ARB_TIME_SLOT_7             (C_ARB_TIME_SLOT_7), 
   .C_ARB_TIME_SLOT_8             (C_ARB_TIME_SLOT_8), 
   .C_ARB_TIME_SLOT_9             (C_ARB_TIME_SLOT_9), 
   .C_ARB_TIME_SLOT_10            (C_ARB_TIME_SLOT_10), 
   .C_ARB_TIME_SLOT_11            (C_ARB_TIME_SLOT_11), 
   .C_PORT_CONFIG                 (C_PORT_CONFIG), 
   .C_MEM_TRAS                    (C_MEM_TRAS), 
   .C_MEM_TRCD                    (C_MEM_TRCD), 
   .C_MEM_TREFI                   (C_MEM_TREFI), 
   .C_MEM_TRFC                    (C_MEM_TRFC), 
   .C_MEM_TRP                     (C_MEM_TRP), 
   .C_MEM_TWR                     (C_MEM_TWR), 
   .C_MEM_TRTP                    (C_MEM_TRTP), 
   .C_MEM_TWTR                    (C_MEM_TWTR), 
   .C_NUM_DQ_PINS                 (C_NUM_DQ_PINS), 
   .C_MEM_TYPE                    (C_MEM_TYPE), 
   .C_MEM_DENSITY                 (C_MEM_DENSITY), 
   .C_MEM_BURST_LEN               (C_MEM_BURST_LEN), 
   .C_MEM_CAS_LATENCY             (C_MEM_CAS_LATENCY), 
   .C_MEM_ADDR_WIDTH              (C_MEM_ADDR_WIDTH), 
   .C_MEM_BANKADDR_WIDTH          (C_MEM_BANKADDR_WIDTH), 
   .C_MEM_NUM_COL_BITS            (C_MEM_NUM_COL_BITS), 
   .C_MEM_DDR3_CAS_LATENCY        (C_MEM_DDR3_CAS_LATENCY), 
   .C_MEM_MOBILE_PA_SR            (C_MEM_MOBILE_PA_SR), 
   .C_MEM_DDR1_2_ODS              (C_MEM_DDR1_2_ODS), 
   .C_MEM_DDR3_ODS                (C_MEM_DDR3_ODS), 
   .C_MEM_DDR2_RTT                (C_MEM_DDR2_RTT), 
   .C_MEM_DDR3_RTT                (C_MEM_DDR3_RTT), 
   .C_MEM_MDDR_ODS                (C_MEM_MDDR_ODS), 
   .C_MEM_DDR2_DIFF_DQS_EN        (C_MEM_DDR2_DIFF_DQS_EN), 
   .C_MEM_DDR2_3_PA_SR            (C_MEM_DDR2_3_PA_SR), 
   .C_MEM_DDR3_CAS_WR_LATENCY     (C_MEM_DDR3_CAS_WR_LATENCY), 
   .C_MEM_DDR3_AUTO_SR            (C_MEM_DDR3_AUTO_SR), 
   .C_MEM_DDR2_3_HIGH_TEMP_SR     (C_MEM_DDR2_3_HIGH_TEMP_SR), 
   .C_MEM_DDR3_DYN_WRT_ODT        (C_MEM_DDR3_DYN_WRT_ODT), 
   .C_MEM_TZQINIT_MAXCNT          (C_MEM_TZQINIT_MAXCNT), 
   .C_MC_CALIB_BYPASS             (C_MC_CALIB_BYPASS), 
   .C_MC_CALIBRATION_RA           (C_MC_CALIBRATION_RA),
   .C_MC_CALIBRATION_BA           (C_MC_CALIBRATION_BA),
   .C_MC_CALIBRATION_CA           (C_MC_CALIBRATION_CA),
   .C_CALIB_SOFT_IP               (C_CALIB_SOFT_IP), 
   .C_SKIP_IN_TERM_CAL            (C_SKIP_IN_TERM_CAL), 
   .C_SKIP_DYNAMIC_CAL            (C_SKIP_DYNAMIC_CAL), 
   .C_SKIP_DYN_IN_TERM            (C_SKIP_DYN_IN_TERM), 
   .LDQSP_TAP_DELAY_VAL           (LDQSP_TAP_DELAY_VAL), 
   .UDQSP_TAP_DELAY_VAL           (UDQSP_TAP_DELAY_VAL), 
   .LDQSN_TAP_DELAY_VAL           (LDQSN_TAP_DELAY_VAL), 
   .UDQSN_TAP_DELAY_VAL           (UDQSN_TAP_DELAY_VAL), 
   .DQ0_TAP_DELAY_VAL             (DQ0_TAP_DELAY_VAL), 
   .DQ1_TAP_DELAY_VAL             (DQ1_TAP_DELAY_VAL), 
   .DQ2_TAP_DELAY_VAL             (DQ2_TAP_DELAY_VAL), 
   .DQ3_TAP_DELAY_VAL             (DQ3_TAP_DELAY_VAL), 
   .DQ4_TAP_DELAY_VAL             (DQ4_TAP_DELAY_VAL), 
   .DQ5_TAP_DELAY_VAL             (DQ5_TAP_DELAY_VAL), 
   .DQ6_TAP_DELAY_VAL             (DQ6_TAP_DELAY_VAL), 
   .DQ7_TAP_DELAY_VAL             (DQ7_TAP_DELAY_VAL), 
   .DQ8_TAP_DELAY_VAL             (DQ8_TAP_DELAY_VAL), 
   .DQ9_TAP_DELAY_VAL             (DQ9_TAP_DELAY_VAL), 
   .DQ10_TAP_DELAY_VAL            (DQ10_TAP_DELAY_VAL), 
   .DQ11_TAP_DELAY_VAL            (DQ11_TAP_DELAY_VAL), 
   .DQ12_TAP_DELAY_VAL            (DQ12_TAP_DELAY_VAL), 
   .DQ13_TAP_DELAY_VAL            (DQ13_TAP_DELAY_VAL), 
   .DQ14_TAP_DELAY_VAL            (DQ14_TAP_DELAY_VAL), 
   .DQ15_TAP_DELAY_VAL            (DQ15_TAP_DELAY_VAL), 
   .C_MC_CALIBRATION_CLK_DIV      (C_MC_CALIBRATION_CLK_DIV), 
   .C_MC_CALIBRATION_MODE         (C_MC_CALIBRATION_MODE), 
   .C_MC_CALIBRATION_DELAY        (C_MC_CALIBRATION_DELAY),
   .C_SIMULATION                  (C_SIMULATION), 
   .C_P0_MASK_SIZE                (C_P0_MASK_SIZE), 
   .C_P0_DATA_PORT_SIZE           (C_P0_DATA_PORT_SIZE), 
   .C_P1_MASK_SIZE                (C_P1_MASK_SIZE), 
   .C_P1_DATA_PORT_SIZE           (C_P1_DATA_PORT_SIZE), 
   .C_MCB_USE_EXTERNAL_BUFPLL     (C_MCB_USE_EXTERNAL_BUFPLL)
  )
mcb_ui_top_inst
  (
   // Raw Wrapper Signals
   .sysclk_2x                     (sysclk_2x),          
   .sysclk_2x_180                 (sysclk_2x_180), 
   .pll_ce_0                      (pll_ce_0),
   .pll_ce_90                     (pll_ce_90), 
   .pll_lock                      (pll_lock),
   .sysclk_2x_bufpll_o            (sysclk_2x_bufpll_o),     
   .sysclk_2x_180_bufpll_o        (sysclk_2x_180_bufpll_o),
   .pll_ce_0_bufpll_o             (pll_ce_0_bufpll_o),
   .pll_ce_90_bufpll_o            (pll_ce_90_bufpll_o),
   .pll_lock_bufpll_o             (pll_lock_bufpll_o),   
   .sys_rst                       (async_rst),
   .p0_arb_en                     (1'b1), 
   .p0_cmd_clk                    (p0_cmd_clk),
   .p0_cmd_en                     (p0_cmd_en), 
   .p0_cmd_instr                  (p0_cmd_instr),
   .p0_cmd_bl                     (p0_cmd_bl), 
   .p0_cmd_byte_addr              (p0_cmd_byte_addr),       
   .p0_cmd_empty                  (p0_cmd_empty),
   .p0_cmd_full                   (p0_cmd_full),
   .p0_wr_clk                     (p0_wr_clk), 
   .p0_wr_en                      (p0_wr_en),
   .p0_wr_mask                    (p0_wr_mask),
   .p0_wr_data                    (p0_wr_data),
   .p0_wr_full                    (p0_wr_full),
   .p0_wr_empty                   (p0_wr_empty),
   .p0_wr_count                   (p0_wr_count),
   .p0_wr_underrun                (p0_wr_underrun),  
   .p0_wr_error                   (p0_wr_error),
   .p0_rd_clk                     (p0_rd_clk), 
   .p0_rd_en                      (p0_rd_en),
   .p0_rd_data                    (p0_rd_data),
   .p0_rd_full                    (p0_rd_full),
   .p0_rd_empty                   (p0_rd_empty),
   .p0_rd_count                   (p0_rd_count),
   .p0_rd_overflow                (p0_rd_overflow),  
   .p0_rd_error                   (p0_rd_error),
   .p1_arb_en                     (1'b1), 
   .p1_cmd_clk                    (p1_cmd_clk),
   .p1_cmd_en                     (p1_cmd_en), 
   .p1_cmd_instr                  (p1_cmd_instr),
   .p1_cmd_bl                     (p1_cmd_bl), 
   .p1_cmd_byte_addr              (p1_cmd_byte_addr),       
   .p1_cmd_empty                  (p1_cmd_empty),
   .p1_cmd_full                   (p1_cmd_full),
   .p1_wr_clk                     (p1_wr_clk), 
   .p1_wr_en                      (p1_wr_en),
   .p1_wr_mask                    (p1_wr_mask),
   .p1_wr_data                    (p1_wr_data),
   .p1_wr_full                    (p1_wr_full),
   .p1_wr_empty                   (p1_wr_empty),
   .p1_wr_count                   (p1_wr_count),
   .p1_wr_underrun                (p1_wr_underrun),  
   .p1_wr_error                   (p1_wr_error),
   .p1_rd_clk                     (p1_rd_clk), 
   .p1_rd_en                      (p1_rd_en),
   .p1_rd_data                    (p1_rd_data),
   .p1_rd_full                    (p1_rd_full),
   .p1_rd_empty                   (p1_rd_empty),
   .p1_rd_count                   (p1_rd_count),
   .p1_rd_overflow                (p1_rd_overflow),  
   .p1_rd_error                   (p1_rd_error),
   .p2_arb_en                     (1'b1), 
   .p2_cmd_clk                    (p2_cmd_clk),
   .p2_cmd_en                     (p2_cmd_en), 
   .p2_cmd_instr                  (p2_cmd_instr),
   .p2_cmd_bl                     (p2_cmd_bl), 
   .p2_cmd_byte_addr              (p2_cmd_byte_addr),       
   .p2_cmd_empty                  (p2_cmd_empty),
   .p2_cmd_full                   (p2_cmd_full),
   .p2_wr_clk                     (p2_wr_clk), 
   .p2_wr_en                      (p2_wr_en),
   .p2_wr_mask                    (p2_wr_mask),
   .p2_wr_data                    (p2_wr_data),
   .p2_wr_full                    (p2_wr_full),
   .p2_wr_empty                   (p2_wr_empty),
   .p2_wr_count                   (p2_wr_count),
   .p2_wr_underrun                (p2_wr_underrun),  
   .p2_wr_error                   (p2_wr_error),
   .p2_rd_clk                     (p2_rd_clk), 
   .p2_rd_en                      (p2_rd_en),
   .p2_rd_data                    (p2_rd_data),
   .p2_rd_full                    (p2_rd_full),
   .p2_rd_empty                   (p2_rd_empty),
   .p2_rd_count                   (p2_rd_count),
   .p2_rd_overflow                (p2_rd_overflow),  
   .p2_rd_error                   (p2_rd_error),
   .p3_arb_en                     (1'b1), 
   .p3_cmd_clk                    (p3_cmd_clk),
   .p3_cmd_en                     (p3_cmd_en), 
   .p3_cmd_instr                  (p3_cmd_instr),
   .p3_cmd_bl                     (p3_cmd_bl), 
   .p3_cmd_byte_addr              (p3_cmd_byte_addr),       
   .p3_cmd_empty                  (p3_cmd_empty),
   .p3_cmd_full                   (p3_cmd_full),
   .p3_wr_clk                     (p3_wr_clk), 
   .p3_wr_en                      (p3_wr_en),
   .p3_wr_mask                    (p3_wr_mask),
   .p3_wr_data                    (p3_wr_data),
   .p3_wr_full                    (p3_wr_full),
   .p3_wr_empty                   (p3_wr_empty),
   .p3_wr_count                   (p3_wr_count),
   .p3_wr_underrun                (p3_wr_underrun),  
   .p3_wr_error                   (p3_wr_error),
   .p3_rd_clk                     (p3_rd_clk), 
   .p3_rd_en                      (p3_rd_en),
   .p3_rd_data                    (p3_rd_data),
   .p3_rd_full                    (p3_rd_full),
   .p3_rd_empty                   (p3_rd_empty),
   .p3_rd_count                   (p3_rd_count),
   .p3_rd_overflow                (p3_rd_overflow),  
   .p3_rd_error                   (p3_rd_error),
   .p4_arb_en                     (1'b1), 
   .p4_cmd_clk                    (p4_cmd_clk),
   .p4_cmd_en                     (p4_cmd_en), 
   .p4_cmd_instr                  (p4_cmd_instr),
   .p4_cmd_bl                     (p4_cmd_bl), 
   .p4_cmd_byte_addr              (p4_cmd_byte_addr),       
   .p4_cmd_empty                  (p4_cmd_empty),
   .p4_cmd_full                   (p4_cmd_full),
   .p4_wr_clk                     (p4_wr_clk), 
   .p4_wr_en                      (p4_wr_en),
   .p4_wr_mask                    (p4_wr_mask),
   .p4_wr_data                    (p4_wr_data),
   .p4_wr_full                    (p4_wr_full),
   .p4_wr_empty                   (p4_wr_empty),
   .p4_wr_count                   (p4_wr_count),
   .p4_wr_underrun                (p4_wr_underrun),  
   .p4_wr_error                   (p4_wr_error),
   .p4_rd_clk                     (p4_rd_clk), 
   .p4_rd_en                      (p4_rd_en),
   .p4_rd_data                    (p4_rd_data),
   .p4_rd_full                    (p4_rd_full),
   .p4_rd_empty                   (p4_rd_empty),
   .p4_rd_count                   (p4_rd_count),
   .p4_rd_overflow                (p4_rd_overflow),  
   .p4_rd_error                   (p4_rd_error),
   .p5_arb_en                     (1'b1), 
   .p5_cmd_clk                    (p5_cmd_clk),
   .p5_cmd_en                     (p5_cmd_en), 
   .p5_cmd_instr                  (p5_cmd_instr),
   .p5_cmd_bl                     (p5_cmd_bl), 
   .p5_cmd_byte_addr              (p5_cmd_byte_addr),       
   .p5_cmd_empty                  (p5_cmd_empty),
   .p5_cmd_full                   (p5_cmd_full),
   .p5_wr_clk                     (p5_wr_clk), 
   .p5_wr_en                      (p5_wr_en),
   .p5_wr_mask                    (p5_wr_mask),
   .p5_wr_data                    (p5_wr_data),
   .p5_wr_full                    (p5_wr_full),
   .p5_wr_empty                   (p5_wr_empty),
   .p5_wr_count                   (p5_wr_count),
   .p5_wr_underrun                (p5_wr_underrun),  
   .p5_wr_error                   (p5_wr_error),
   .p5_rd_clk                     (p5_rd_clk), 
   .p5_rd_en                      (p5_rd_en),
   .p5_rd_data                    (p5_rd_data),
   .p5_rd_full                    (p5_rd_full),
   .p5_rd_empty                   (p5_rd_empty),
   .p5_rd_count                   (p5_rd_count),
   .p5_rd_overflow                (p5_rd_overflow),  
   .p5_rd_error                   (p5_rd_error),
   .mcbx_dram_addr                (mcbx_dram_addr),  
   .mcbx_dram_ba                  (mcbx_dram_ba),
   .mcbx_dram_ras_n               (mcbx_dram_ras_n),       
   .mcbx_dram_cas_n               (mcbx_dram_cas_n),       
   .mcbx_dram_we_n                (mcbx_dram_we_n),  
   .mcbx_dram_cke                 (mcbx_dram_cke), 
   .mcbx_dram_clk                 (mcbx_dram_clk), 
   .mcbx_dram_clk_n               (mcbx_dram_clk_n),       
   .mcbx_dram_dq                  (mcbx_dram_dq),
   .mcbx_dram_dqs                 (mcbx_dram_dqs), 
   .mcbx_dram_dqs_n               (mcbx_dram_dqs_n),       
   .mcbx_dram_udqs                (mcbx_dram_udqs),  
   .mcbx_dram_udqs_n              (mcbx_dram_udqs_n),       
   .mcbx_dram_udm                 (mcbx_dram_udm), 
   .mcbx_dram_ldm                 (mcbx_dram_ldm), 
   .mcbx_dram_odt                 (mcbx_dram_odt), 
   .mcbx_dram_ddr3_rst            (mcbx_dram_ddr3_rst),      
   .calib_recal                   (1'b0),
   .rzq                           (mcbx_rzq),
   .zio                           (mcbx_zio),
   .ui_read                       (1'b0),
   .ui_add                        (1'b0),
   .ui_cs                         (1'b0),
   .ui_clk                        (mcb_drp_clk),
   .ui_sdi                        (1'b0),
   .ui_addr                       (5'b0),
   .ui_broadcast                  (1'b0),
   .ui_drp_update                 (1'b0), 
   .ui_done_cal                   (1'b1),
   .ui_cmd                        (1'b0),
   .ui_cmd_in                     (1'b0), 
   .ui_cmd_en                     (1'b0), 
   .ui_dqcount                    (4'b0),
   .ui_dq_lower_dec               (1'b0),       
   .ui_dq_lower_inc               (1'b0),       
   .ui_dq_upper_dec               (1'b0),       
   .ui_dq_upper_inc               (1'b0),       
   .ui_udqs_inc                   (1'b0),
   .ui_udqs_dec                   (1'b0),
   .ui_ldqs_inc                   (1'b0),
   .ui_ldqs_dec                   (1'b0),
   .uo_data                       (uo_data),
   .uo_data_valid                 (uo_data_valid), 
   .uo_done_cal                   (calib_done),
   .uo_cmd_ready_in               (uo_cmd_ready_in),       
   .uo_refrsh_flag                (uo_refrsh_flag),  
   .uo_cal_start                  (uo_cal_start),
   .uo_sdo                        (uo_sdo),
   .status                        (status),
   .selfrefresh_enter             (selfrefresh_enter),       
   .selfrefresh_mode              (selfrefresh_mode),

   // AXI Signals                 
   .s0_axi_aclk                   (s0_axi_aclk),
   .s0_axi_aresetn                (s0_axi_aresetn),
   .s0_axi_awid                   (s0_axi_awid), 
   .s0_axi_awaddr                 (s0_axi_awaddr), 
   .s0_axi_awlen                  (s0_axi_awlen), 
   .s0_axi_awsize                 (s0_axi_awsize), 
   .s0_axi_awburst                (s0_axi_awburst), 
   .s0_axi_awlock                 (s0_axi_awlock), 
   .s0_axi_awcache                (s0_axi_awcache), 
   .s0_axi_awprot                 (s0_axi_awprot), 
   .s0_axi_awqos                  (s0_axi_awqos), 
   .s0_axi_awvalid                (s0_axi_awvalid), 
   .s0_axi_awready                (s0_axi_awready), 
   .s0_axi_wdata                  (s0_axi_wdata), 
   .s0_axi_wstrb                  (s0_axi_wstrb), 
   .s0_axi_wlast                  (s0_axi_wlast), 
   .s0_axi_wvalid                 (s0_axi_wvalid), 
   .s0_axi_wready                 (s0_axi_wready), 
   .s0_axi_bid                    (s0_axi_bid), 
   .s0_axi_bresp                  (s0_axi_bresp), 
   .s0_axi_bvalid                 (s0_axi_bvalid), 
   .s0_axi_bready                 (s0_axi_bready), 
   .s0_axi_arid                   (s0_axi_arid), 
   .s0_axi_araddr                 (s0_axi_araddr), 
   .s0_axi_arlen                  (s0_axi_arlen), 
   .s0_axi_arsize                 (s0_axi_arsize), 
   .s0_axi_arburst                (s0_axi_arburst), 
   .s0_axi_arlock                 (s0_axi_arlock), 
   .s0_axi_arcache                (s0_axi_arcache), 
   .s0_axi_arprot                 (s0_axi_arprot), 
   .s0_axi_arqos                  (s0_axi_arqos), 
   .s0_axi_arvalid                (s0_axi_arvalid), 
   .s0_axi_arready                (s0_axi_arready), 
   .s0_axi_rid                    (s0_axi_rid), 
   .s0_axi_rdata                  (s0_axi_rdata), 
   .s0_axi_rresp                  (s0_axi_rresp), 
   .s0_axi_rlast                  (s0_axi_rlast), 
   .s0_axi_rvalid                 (s0_axi_rvalid), 
   .s0_axi_rready                 (s0_axi_rready),
                                                   
   .s1_axi_aclk                   (s1_axi_aclk),
   .s1_axi_aresetn                (s1_axi_aresetn),
   .s1_axi_awid                   (s1_axi_awid), 
   .s1_axi_awaddr                 (s1_axi_awaddr), 
   .s1_axi_awlen                  (s1_axi_awlen), 
   .s1_axi_awsize                 (s1_axi_awsize), 
   .s1_axi_awburst                (s1_axi_awburst), 
   .s1_axi_awlock                 (s1_axi_awlock), 
   .s1_axi_awcache                (s1_axi_awcache), 
   .s1_axi_awprot                 (s1_axi_awprot), 
   .s1_axi_awqos                  (s1_axi_awqos), 
   .s1_axi_awvalid                (s1_axi_awvalid), 
   .s1_axi_awready                (s1_axi_awready), 
   .s1_axi_wdata                  (s1_axi_wdata), 
   .s1_axi_wstrb                  (s1_axi_wstrb), 
   .s1_axi_wlast                  (s1_axi_wlast), 
   .s1_axi_wvalid                 (s1_axi_wvalid), 
   .s1_axi_wready                 (s1_axi_wready), 
   .s1_axi_bid                    (s1_axi_bid), 
   .s1_axi_bresp                  (s1_axi_bresp), 
   .s1_axi_bvalid                 (s1_axi_bvalid), 
   .s1_axi_bready                 (s1_axi_bready), 
   .s1_axi_arid                   (s1_axi_arid), 
   .s1_axi_araddr                 (s1_axi_araddr), 
   .s1_axi_arlen                  (s1_axi_arlen), 
   .s1_axi_arsize                 (s1_axi_arsize), 
   .s1_axi_arburst                (s1_axi_arburst), 
   .s1_axi_arlock                 (s1_axi_arlock), 
   .s1_axi_arcache                (s1_axi_arcache), 
   .s1_axi_arprot                 (s1_axi_arprot), 
   .s1_axi_arqos                  (s1_axi_arqos), 
   .s1_axi_arvalid                (s1_axi_arvalid), 
   .s1_axi_arready                (s1_axi_arready), 
   .s1_axi_rid                    (s1_axi_rid), 
   .s1_axi_rdata                  (s1_axi_rdata), 
   .s1_axi_rresp                  (s1_axi_rresp), 
   .s1_axi_rlast                  (s1_axi_rlast), 
   .s1_axi_rvalid                 (s1_axi_rvalid), 
   .s1_axi_rready                 (s1_axi_rready),
                                                   
   .s2_axi_aclk                   (s2_axi_aclk),
   .s2_axi_aresetn                (s2_axi_aresetn),
   .s2_axi_awid                   (s2_axi_awid), 
   .s2_axi_awaddr                 (s2_axi_awaddr), 
   .s2_axi_awlen                  (s2_axi_awlen), 
   .s2_axi_awsize                 (s2_axi_awsize), 
   .s2_axi_awburst                (s2_axi_awburst), 
   .s2_axi_awlock                 (s2_axi_awlock), 
   .s2_axi_awcache                (s2_axi_awcache), 
   .s2_axi_awprot                 (s2_axi_awprot), 
   .s2_axi_awqos                  (s2_axi_awqos), 
   .s2_axi_awvalid                (s2_axi_awvalid), 
   .s2_axi_awready                (s2_axi_awready), 
   .s2_axi_wdata                  (s2_axi_wdata), 
   .s2_axi_wstrb                  (s2_axi_wstrb), 
   .s2_axi_wlast                  (s2_axi_wlast), 
   .s2_axi_wvalid                 (s2_axi_wvalid), 
   .s2_axi_wready                 (s2_axi_wready), 
   .s2_axi_bid                    (s2_axi_bid), 
   .s2_axi_bresp                  (s2_axi_bresp), 
   .s2_axi_bvalid                 (s2_axi_bvalid), 
   .s2_axi_bready                 (s2_axi_bready), 
   .s2_axi_arid                   (s2_axi_arid), 
   .s2_axi_araddr                 (s2_axi_araddr), 
   .s2_axi_arlen                  (s2_axi_arlen), 
   .s2_axi_arsize                 (s2_axi_arsize), 
   .s2_axi_arburst                (s2_axi_arburst), 
   .s2_axi_arlock                 (s2_axi_arlock), 
   .s2_axi_arcache                (s2_axi_arcache), 
   .s2_axi_arprot                 (s2_axi_arprot), 
   .s2_axi_arqos                  (s2_axi_arqos), 
   .s2_axi_arvalid                (s2_axi_arvalid), 
   .s2_axi_arready                (s2_axi_arready), 
   .s2_axi_rid                    (s2_axi_rid), 
   .s2_axi_rdata                  (s2_axi_rdata), 
   .s2_axi_rresp                  (s2_axi_rresp), 
   .s2_axi_rlast                  (s2_axi_rlast), 
   .s2_axi_rvalid                 (s2_axi_rvalid), 
   .s2_axi_rready                 (s2_axi_rready),
                                                   
   .s3_axi_aclk                   (s3_axi_aclk),
   .s3_axi_aresetn                (s3_axi_aresetn),
   .s3_axi_awid                   (s3_axi_awid), 
   .s3_axi_awaddr                 (s3_axi_awaddr), 
   .s3_axi_awlen                  (s3_axi_awlen), 
   .s3_axi_awsize                 (s3_axi_awsize), 
   .s3_axi_awburst                (s3_axi_awburst), 
   .s3_axi_awlock                 (s3_axi_awlock), 
   .s3_axi_awcache                (s3_axi_awcache), 
   .s3_axi_awprot                 (s3_axi_awprot), 
   .s3_axi_awqos                  (s3_axi_awqos), 
   .s3_axi_awvalid                (s3_axi_awvalid), 
   .s3_axi_awready                (s3_axi_awready), 
   .s3_axi_wdata                  (s3_axi_wdata), 
   .s3_axi_wstrb                  (s3_axi_wstrb), 
   .s3_axi_wlast                  (s3_axi_wlast), 
   .s3_axi_wvalid                 (s3_axi_wvalid), 
   .s3_axi_wready                 (s3_axi_wready), 
   .s3_axi_bid                    (s3_axi_bid), 
   .s3_axi_bresp                  (s3_axi_bresp), 
   .s3_axi_bvalid                 (s3_axi_bvalid), 
   .s3_axi_bready                 (s3_axi_bready), 
   .s3_axi_arid                   (s3_axi_arid), 
   .s3_axi_araddr                 (s3_axi_araddr), 
   .s3_axi_arlen                  (s3_axi_arlen), 
   .s3_axi_arsize                 (s3_axi_arsize), 
   .s3_axi_arburst                (s3_axi_arburst), 
   .s3_axi_arlock                 (s3_axi_arlock), 
   .s3_axi_arcache                (s3_axi_arcache), 
   .s3_axi_arprot                 (s3_axi_arprot), 
   .s3_axi_arqos                  (s3_axi_arqos), 
   .s3_axi_arvalid                (s3_axi_arvalid), 
   .s3_axi_arready                (s3_axi_arready), 
   .s3_axi_rid                    (s3_axi_rid), 
   .s3_axi_rdata                  (s3_axi_rdata), 
   .s3_axi_rresp                  (s3_axi_rresp), 
   .s3_axi_rlast                  (s3_axi_rlast), 
   .s3_axi_rvalid                 (s3_axi_rvalid), 
   .s3_axi_rready                 (s3_axi_rready),
                                                   
   .s4_axi_aclk                   (s4_axi_aclk),
   .s4_axi_aresetn                (s4_axi_aresetn),
   .s4_axi_awid                   (s4_axi_awid), 
   .s4_axi_awaddr                 (s4_axi_awaddr), 
   .s4_axi_awlen                  (s4_axi_awlen), 
   .s4_axi_awsize                 (s4_axi_awsize), 
   .s4_axi_awburst                (s4_axi_awburst), 
   .s4_axi_awlock                 (s4_axi_awlock), 
   .s4_axi_awcache                (s4_axi_awcache), 
   .s4_axi_awprot                 (s4_axi_awprot), 
   .s4_axi_awqos                  (s4_axi_awqos), 
   .s4_axi_awvalid                (s4_axi_awvalid), 
   .s4_axi_awready                (s4_axi_awready), 
   .s4_axi_wdata                  (s4_axi_wdata), 
   .s4_axi_wstrb                  (s4_axi_wstrb), 
   .s4_axi_wlast                  (s4_axi_wlast), 
   .s4_axi_wvalid                 (s4_axi_wvalid), 
   .s4_axi_wready                 (s4_axi_wready), 
   .s4_axi_bid                    (s4_axi_bid), 
   .s4_axi_bresp                  (s4_axi_bresp), 
   .s4_axi_bvalid                 (s4_axi_bvalid), 
   .s4_axi_bready                 (s4_axi_bready), 
   .s4_axi_arid                   (s4_axi_arid), 
   .s4_axi_araddr                 (s4_axi_araddr), 
   .s4_axi_arlen                  (s4_axi_arlen), 
   .s4_axi_arsize                 (s4_axi_arsize), 
   .s4_axi_arburst                (s4_axi_arburst), 
   .s4_axi_arlock                 (s4_axi_arlock), 
   .s4_axi_arcache                (s4_axi_arcache), 
   .s4_axi_arprot                 (s4_axi_arprot), 
   .s4_axi_arqos                  (s4_axi_arqos), 
   .s4_axi_arvalid                (s4_axi_arvalid), 
   .s4_axi_arready                (s4_axi_arready), 
   .s4_axi_rid                    (s4_axi_rid), 
   .s4_axi_rdata                  (s4_axi_rdata), 
   .s4_axi_rresp                  (s4_axi_rresp), 
   .s4_axi_rlast                  (s4_axi_rlast), 
   .s4_axi_rvalid                 (s4_axi_rvalid), 
   .s4_axi_rready                 (s4_axi_rready),
                                                   
   .s5_axi_aclk                   (s5_axi_aclk),
   .s5_axi_aresetn                (s5_axi_aresetn),
   .s5_axi_awid                   (s5_axi_awid), 
   .s5_axi_awaddr                 (s5_axi_awaddr), 
   .s5_axi_awlen                  (s5_axi_awlen), 
   .s5_axi_awsize                 (s5_axi_awsize), 
   .s5_axi_awburst                (s5_axi_awburst), 
   .s5_axi_awlock                 (s5_axi_awlock), 
   .s5_axi_awcache                (s5_axi_awcache), 
   .s5_axi_awprot                 (s5_axi_awprot), 
   .s5_axi_awqos                  (s5_axi_awqos), 
   .s5_axi_awvalid                (s5_axi_awvalid), 
   .s5_axi_awready                (s5_axi_awready), 
   .s5_axi_wdata                  (s5_axi_wdata), 
   .s5_axi_wstrb                  (s5_axi_wstrb), 
   .s5_axi_wlast                  (s5_axi_wlast), 
   .s5_axi_wvalid                 (s5_axi_wvalid), 
   .s5_axi_wready                 (s5_axi_wready), 
   .s5_axi_bid                    (s5_axi_bid), 
   .s5_axi_bresp                  (s5_axi_bresp), 
   .s5_axi_bvalid                 (s5_axi_bvalid), 
   .s5_axi_bready                 (s5_axi_bready), 
   .s5_axi_arid                   (s5_axi_arid), 
   .s5_axi_araddr                 (s5_axi_araddr), 
   .s5_axi_arlen                  (s5_axi_arlen), 
   .s5_axi_arsize                 (s5_axi_arsize), 
   .s5_axi_arburst                (s5_axi_arburst), 
   .s5_axi_arlock                 (s5_axi_arlock), 
   .s5_axi_arcache                (s5_axi_arcache), 
   .s5_axi_arprot                 (s5_axi_arprot), 
   .s5_axi_arqos                  (s5_axi_arqos), 
   .s5_axi_arvalid                (s5_axi_arvalid), 
   .s5_axi_arready                (s5_axi_arready), 
   .s5_axi_rid                    (s5_axi_rid), 
   .s5_axi_rdata                  (s5_axi_rdata), 
   .s5_axi_rresp                  (s5_axi_rresp), 
   .s5_axi_rlast                  (s5_axi_rlast), 
   .s5_axi_rvalid                 (s5_axi_rvalid), 
   .s5_axi_rready                 (s5_axi_rready)
  );

endmodule

module iodrp_controller(
  input   wire  [7:0] memcell_address,
  input   wire  [7:0] write_data,
  output  reg   [7:0] read_data,
  input   wire        rd_not_write,
  input   wire        cmd_valid,
  output  wire        rdy_busy_n,
  input   wire        use_broadcast,
  input   wire        sync_rst,
  input   wire        DRP_CLK,
  output  reg         DRP_CS,
  output  wire        DRP_SDI,  //output to IODRP SDI pin
  output  reg         DRP_ADD,
  output  reg         DRP_BKST,
  input   wire        DRP_SDO   //input from IODRP SDO pin
  );

  reg   [7:0]   memcell_addr_reg;     // Register where memcell_address is captured during the READY state
  reg   [7:0]   data_reg;             // Register which stores the write data until it is ready to be shifted out
  reg   [7:0]   shift_through_reg;    // The shift register which shifts out SDO and shifts in SDI.
                                      // This register is loaded before the address or data phase, but continues
                                      // to shift for a writeback of read data
  reg           load_shift_n;         // The signal which causes shift_through_reg to load the new value from data_out_mux, or continue to shift data in from DRP_SDO
  reg           addr_data_sel_n;      // The signal which indicates where the shift_through_reg should load from.  0 -> data_reg  1 -> memcell_addr_reg
  reg   [2:0]   bit_cnt;              // The counter for which bit is being shifted during address or data phase
  reg           rd_not_write_reg;
  reg           AddressPhase;         // This is set after the first address phase has executed
  reg           capture_read_data;

  (* FSM_ENCODING="one-hot" *) reg [2:0] state, nextstate;

  wire  [7:0]   data_out_mux;         // The mux which selects between data_reg and memcell_addr_reg for sending to shift_through_reg
  wire          DRP_SDI_pre;          // added so that DRP_SDI output is only active when DRP_CS is active

  localparam  READY             = 3'h0;
  localparam  DECIDE            = 3'h1;
  localparam  ADDR_PHASE        = 3'h2;
  localparam  ADDR_TO_DATA_GAP  = 3'h3;
  localparam  ADDR_TO_DATA_GAP2 = 3'h4;
  localparam  ADDR_TO_DATA_GAP3 = 3'h5;
  localparam  DATA_PHASE        = 3'h6;
  localparam  ALMOST_READY      = 3'h7;

  localparam  IOI_DQ0           = 5'h01;
  localparam  IOI_DQ1           = 5'h00;
  localparam  IOI_DQ2           = 5'h03;
  localparam  IOI_DQ3           = 5'h02;
  localparam  IOI_DQ4           = 5'h05;
  localparam  IOI_DQ5           = 5'h04;
  localparam  IOI_DQ6           = 5'h07;
  localparam  IOI_DQ7           = 5'h06;
  localparam  IOI_DQ8           = 5'h09;
  localparam  IOI_DQ9           = 5'h08;
  localparam  IOI_DQ10          = 5'h0B;
  localparam  IOI_DQ11          = 5'h0A;
  localparam  IOI_DQ12          = 5'h0D;
  localparam  IOI_DQ13          = 5'h0C;
  localparam  IOI_DQ14          = 5'h0F;
  localparam  IOI_DQ15          = 5'h0E;
  localparam  IOI_UDQS_CLK      = 5'h1D;
  localparam  IOI_UDQS_PIN      = 5'h1C;
  localparam  IOI_LDQS_CLK      = 5'h1F;
  localparam  IOI_LDQS_PIN      = 5'h1E;
  //synthesis translate_off
  reg   [32*8-1:0]  state_ascii;
  always @ (state) begin
    case (state)
      READY             :state_ascii  <= "READY";
      DECIDE            :state_ascii  <= "DECIDE";
      ADDR_PHASE        :state_ascii  <= "ADDR_PHASE";
      ADDR_TO_DATA_GAP  :state_ascii  <= "ADDR_TO_DATA_GAP";
      ADDR_TO_DATA_GAP2 :state_ascii  <= "ADDR_TO_DATA_GAP2";
      ADDR_TO_DATA_GAP3 :state_ascii  <= "ADDR_TO_DATA_GAP3";
      DATA_PHASE        :state_ascii  <= "DATA_PHASE";
      ALMOST_READY      :state_ascii  <= "ALMOST_READY";
    endcase // case(state)
  end
  //synthesis translate_on
  /*********************************************
   *   Input Registers
   *********************************************/
  always @ (posedge DRP_CLK) begin
     if(state == READY) begin
       memcell_addr_reg <= memcell_address;
       data_reg <= write_data;
       rd_not_write_reg <= rd_not_write;
     end
  end

  assign rdy_busy_n = (state == READY);


  /*********************************************
   *   Shift Registers / Bit Counter
   *********************************************/
  assign data_out_mux = addr_data_sel_n ? memcell_addr_reg : data_reg;

  always @ (posedge DRP_CLK) begin
    if(sync_rst)
      shift_through_reg <= 8'b0;
    else begin
      if (load_shift_n)     //Assume the shifter is either loading or shifting, bit 0 is shifted out first
        shift_through_reg <= data_out_mux;
      else
        shift_through_reg <= {DRP_SDO, shift_through_reg[7:1]};
    end
  end

  always @ (posedge DRP_CLK) begin
    if (((state == ADDR_PHASE) | (state == DATA_PHASE)) & !sync_rst)
      bit_cnt <= bit_cnt + 1;
    else
      bit_cnt <= 3'b000;
  end

  always @ (posedge DRP_CLK) begin
    if(sync_rst) begin
      read_data   <= 8'h00;
//     capture_read_data <= 1'b0;
    end
    else begin
//       capture_read_data <= (state == DATA_PHASE);
//       if(capture_read_data)
      if(state == ALMOST_READY)
        read_data <= shift_through_reg;
//      else
//        read_data <= read_data;
    end
  end

  always @ (posedge DRP_CLK) begin
    if(sync_rst) begin
      AddressPhase  <= 1'b0;
    end
    else begin
      if (AddressPhase) begin
        // Keep it set until we finish the cycle
        AddressPhase <= AddressPhase && ~(state == ALMOST_READY);
      end
      else begin
        // set the address phase when ever we finish the address phase
        AddressPhase <= (state == ADDR_PHASE) && (bit_cnt == 3'b111);
      end
    end
  end

  /*********************************************
   *   DRP Signals
   *********************************************/
  always @ (posedge DRP_CLK) begin
    DRP_ADD     <= (nextstate == ADDR_PHASE);
    DRP_CS      <= (nextstate == ADDR_PHASE) | (nextstate == DATA_PHASE);
    if (state == READY)
      DRP_BKST  <= use_broadcast;
  end

//  assign DRP_SDI_pre  = (DRP_CS)? shift_through_reg[0] : 1'b0;  //if DRP_CS is inactive, just drive 0 out - this is a possible place to pipeline for increased performance
//  assign DRP_SDI      = (rd_not_write_reg & DRP_CS & !DRP_ADD)? DRP_SDO : DRP_SDI_pre; //If reading, then feed SDI back out SDO - this is a possible place to pipeline for increased performance
  assign DRP_SDI = shift_through_reg[0]; // The new read method only requires that we shift out the address and the write data

  /*********************************************
   *   State Machine
   *********************************************/
  always @ (*) begin
    addr_data_sel_n = 1'b0;
    load_shift_n    = 1'b0;
    case (state)
      READY:  begin
        if(cmd_valid)
          nextstate   = DECIDE;
        else
          nextstate   = READY;
      end
      DECIDE: begin
        load_shift_n    = 1;
        addr_data_sel_n = 1;
        nextstate       = ADDR_PHASE;
      end
      ADDR_PHASE: begin
        if(&bit_cnt)
          if (rd_not_write_reg)
            if (AddressPhase)
              // After the second pass go to end of statemachine
              nextstate = ALMOST_READY;
            else
              // execute a second address phase for the read access.
              nextstate = DECIDE;
          else
            nextstate = ADDR_TO_DATA_GAP;
        else
          nextstate   = ADDR_PHASE;
      end
      ADDR_TO_DATA_GAP: begin
        load_shift_n  = 1;
        nextstate     = ADDR_TO_DATA_GAP2;
      end
      ADDR_TO_DATA_GAP2: begin
        load_shift_n  = 1;
        nextstate     = ADDR_TO_DATA_GAP3;
      end
      ADDR_TO_DATA_GAP3: begin
        load_shift_n  = 1;
        nextstate     = DATA_PHASE;
      end
      DATA_PHASE: begin
        if(&bit_cnt)
          nextstate   = ALMOST_READY;
        else
          nextstate   = DATA_PHASE;
      end
      ALMOST_READY: begin
        nextstate     = READY;
      end
      default: begin
        nextstate     = READY;
      end
    endcase
  end

  always @ (posedge DRP_CLK) begin
    if(sync_rst)
      state <= READY;
    else
      state <= nextstate;
  end

endmodule

`ifdef ALTERNATE_READ
`else
  `define ALTERNATE_READ 1'b1
`endif

module iodrp_mcb_controller(
  input   wire  [7:0] memcell_address,
  input   wire  [7:0] write_data,
  output  reg   [7:0] read_data = 0,
  input   wire        rd_not_write,
  input   wire        cmd_valid,
  output  wire        rdy_busy_n,
  input   wire        use_broadcast,
  input   wire  [4:0] drp_ioi_addr,
  input   wire        sync_rst,
  input   wire        DRP_CLK,
  output  reg         DRP_CS,
  output  wire        DRP_SDI,  //output to IODRP SDI pin
  output  reg         DRP_ADD,
  output  reg         DRP_BKST,
  input   wire        DRP_SDO,   //input from IODRP SDO pin
  output  reg         MCB_UIREAD = 1'b0
  );

   reg [7:0]          memcell_addr_reg;     // Register where memcell_address is captured during the READY state
   reg [7:0]          data_reg;             // Register which stores the write data until it is ready to be shifted out
   reg [8:0]          shift_through_reg;    // The shift register which shifts out SDO and shifts in SDI.
                                            //    This register is loaded before the address or data phase, but continues to shift for a writeback of read data
   reg                load_shift_n;         // The signal which causes shift_through_reg to load the new value from data_out_mux, or continue to shift data in from DRP_SDO
   reg                addr_data_sel_n;      // The signal which indicates where the shift_through_reg should load from.  0 -> data_reg  1 -> memcell_addr_reg
   reg [2:0]          bit_cnt= 3'b0;        // The counter for which bit is being shifted during address or data phase
   reg                rd_not_write_reg;
   reg                AddressPhase;         // This is set after the first address phase has executed
   reg                DRP_CS_pre;
   reg                extra_cs;

   (* FSM_ENCODING="GRAY" *) reg [3:0] state, nextstate;

   wire [8:0]   data_out;
   reg  [8:0]   data_out_mux; // The mux which selects between data_reg and memcell_addr_reg for sending to shift_through_reg
   wire DRP_SDI_pre;          //added so that DRP_SDI output is only active when DRP_CS is active

   localparam READY             = 4'h0;
   localparam DECIDE            = 4'h1;
   localparam ADDR_PHASE        = 4'h2;
   localparam ADDR_TO_DATA_GAP  = 4'h3;
   localparam ADDR_TO_DATA_GAP2 = 4'h4;
   localparam ADDR_TO_DATA_GAP3 = 4'h5;
   localparam DATA_PHASE        = 4'h6;
   localparam ALMOST_READY      = 4'h7;
   localparam ALMOST_READY2     = 4'h8;
   localparam ALMOST_READY3     = 4'h9;

   localparam IOI_DQ0           = 5'h01;
   localparam IOI_DQ1           = 5'h00;
   localparam IOI_DQ2           = 5'h03;
   localparam IOI_DQ3           = 5'h02;
   localparam IOI_DQ4           = 5'h05;
   localparam IOI_DQ5           = 5'h04;
   localparam IOI_DQ6           = 5'h07;
   localparam IOI_DQ7           = 5'h06;
   localparam IOI_DQ8           = 5'h09;
   localparam IOI_DQ9           = 5'h08;
   localparam IOI_DQ10          = 5'h0B;
   localparam IOI_DQ11          = 5'h0A;
   localparam IOI_DQ12          = 5'h0D;
   localparam IOI_DQ13          = 5'h0C;
   localparam IOI_DQ14          = 5'h0F;
   localparam IOI_DQ15          = 5'h0E;
   localparam IOI_UDQS_CLK      = 5'h1D;
   localparam IOI_UDQS_PIN      = 5'h1C;
   localparam IOI_LDQS_CLK      = 5'h1F;
   localparam IOI_LDQS_PIN      = 5'h1E;

   //synthesis translate_off
   reg [32*8-1:0] state_ascii;
   always @ (state) begin
      case (state)
  READY     :state_ascii<="READY";
  DECIDE      :state_ascii<="DECIDE";
  ADDR_PHASE    :state_ascii<="ADDR_PHASE";
  ADDR_TO_DATA_GAP  :state_ascii<="ADDR_TO_DATA_GAP";
  ADDR_TO_DATA_GAP2 :state_ascii<="ADDR_TO_DATA_GAP2";
  ADDR_TO_DATA_GAP3 :state_ascii<="ADDR_TO_DATA_GAP3";
  DATA_PHASE    :state_ascii<="DATA_PHASE";
  ALMOST_READY    :state_ascii<="ALMOST_READY";
  ALMOST_READY2   :state_ascii<="ALMOST_READY2";
  ALMOST_READY3   :state_ascii<="ALMOST_READY3";
      endcase // case(state)
   end
   //synthesis translate_on

   /*********************************************
    *   Input Registers
    *********************************************/
   always @ (posedge DRP_CLK) begin
      if(state == READY) begin
        memcell_addr_reg <= memcell_address;
        data_reg <= write_data;
        rd_not_write_reg <= rd_not_write;
      end
   end

   assign rdy_busy_n = (state == READY);

   // The changes below are to compensate for an issue with 1.0 silicon.
   // It may still be necessary to add a clock cycle to the ADD and CS signals

//`define DRP_v1_0_FIX    // Uncomment out this line for synthesis

task shift_n_expand (
  input   [7:0] data_in,
  output  [8:0] data_out
  );

  begin
    if (data_in[0])
      data_out[1:0]  = 2'b11;
    else
      data_out[1:0]  = 2'b00;

    if (data_in[1:0] == 2'b10)
      data_out[2:1]  = 2'b11;
    else
      data_out[2:1]  = {data_in[1], data_out[1]};

    if (data_in[2:1] == 2'b10)
      data_out[3:2]  = 2'b11;
    else
      data_out[3:2]  = {data_in[2], data_out[2]};

    if (data_in[3:2] == 2'b10)
      data_out[4:3]  = 2'b11;
    else
      data_out[4:3]  = {data_in[3], data_out[3]};

    if (data_in[4:3] == 2'b10)
      data_out[5:4]  = 2'b11;
    else
      data_out[5:4]  = {data_in[4], data_out[4]};

    if (data_in[5:4] == 2'b10)
      data_out[6:5]  = 2'b11;
    else
      data_out[6:5]  = {data_in[5], data_out[5]};

    if (data_in[6:5] == 2'b10)
      data_out[7:6]  = 2'b11;
    else
      data_out[7:6]  = {data_in[6], data_out[6]};

    if (data_in[7:6] == 2'b10)
      data_out[8:7]  = 2'b11;
    else
      data_out[8:7]  = {data_in[7], data_out[7]};
  end
endtask


   always @(*) begin
    case(drp_ioi_addr)
`ifdef DRP_v1_0_FIX
      IOI_DQ0       : data_out_mux  = data_out<<1;
      IOI_DQ1       : data_out_mux  = data_out;
      IOI_DQ2       : data_out_mux  = data_out<<1;
//      IOI_DQ2       : data_out_mux  = data_out;
      IOI_DQ3       : data_out_mux  = data_out;
      IOI_DQ4       : data_out_mux  = data_out;
      IOI_DQ5       : data_out_mux  = data_out;
      IOI_DQ6       : shift_n_expand (data_out, data_out_mux);
//      IOI_DQ6       : data_out_mux  = data_out;
      IOI_DQ7       : data_out_mux  = data_out;
      IOI_DQ8       : data_out_mux  = data_out<<1;
      IOI_DQ9       : data_out_mux  = data_out;
      IOI_DQ10      : data_out_mux  = data_out<<1;
      IOI_DQ11      : data_out_mux  = data_out;
      IOI_DQ12      : data_out_mux  = data_out<<1;
      IOI_DQ13      : data_out_mux  = data_out;
      IOI_DQ14      : data_out_mux  = data_out<<1;
      IOI_DQ15      : data_out_mux  = data_out;
      IOI_UDQS_CLK  : data_out_mux  = data_out<<1;
      IOI_UDQS_PIN  : data_out_mux  = data_out<<1;
      IOI_LDQS_CLK  : data_out_mux  = data_out;
      IOI_LDQS_PIN  : data_out_mux  = data_out;
`else
`endif
      IOI_DQ0       : data_out_mux  = data_out;
      IOI_DQ1       : data_out_mux  = data_out;
      IOI_DQ2       : data_out_mux  = data_out;
      IOI_DQ3       : data_out_mux  = data_out;
      IOI_DQ4       : data_out_mux  = data_out;
      IOI_DQ5       : data_out_mux  = data_out;
      IOI_DQ6       : data_out_mux  = data_out;
      IOI_DQ7       : data_out_mux  = data_out;
      IOI_DQ8       : data_out_mux  = data_out;
      IOI_DQ9       : data_out_mux  = data_out;
      IOI_DQ10      : data_out_mux  = data_out;
      IOI_DQ11      : data_out_mux  = data_out;
      IOI_DQ12      : data_out_mux  = data_out;
      IOI_DQ13      : data_out_mux  = data_out;
      IOI_DQ14      : data_out_mux  = data_out;
      IOI_DQ15      : data_out_mux  = data_out;
      IOI_UDQS_CLK  : data_out_mux  = data_out;
      IOI_UDQS_PIN  : data_out_mux  = data_out;
      IOI_LDQS_CLK  : data_out_mux  = data_out;
      IOI_LDQS_PIN  : data_out_mux  = data_out;
      default       : data_out_mux  = data_out;
    endcase
   end


   /*********************************************
    *   Shift Registers / Bit Counter
    *********************************************/
   assign     data_out = (addr_data_sel_n)? {1'b0, memcell_addr_reg} : {1'b0, data_reg};

   always @ (posedge DRP_CLK) begin
      if(sync_rst)
        shift_through_reg <= 9'b0;
      else begin
        if (load_shift_n)     //Assume the shifter is either loading or shifting, bit 0 is shifted out first
          shift_through_reg <= data_out_mux;
        else
          shift_through_reg <= {1'b0, DRP_SDO, shift_through_reg[7:1]};
      end
   end

   always @ (posedge DRP_CLK) begin
      if (((state == ADDR_PHASE) | (state == DATA_PHASE)) & !sync_rst)
        bit_cnt <= bit_cnt + 1;
      else
        bit_cnt <= 3'b0;
   end

  always @ (posedge DRP_CLK) begin
    if(sync_rst) begin
      read_data <= 8'h00;
    end
    else begin
      if(state == ALMOST_READY3)
        read_data <= shift_through_reg;
    end
  end

  always @ (posedge DRP_CLK) begin
    if(sync_rst) begin
      AddressPhase  <= 1'b0;
    end
    else begin
      if (AddressPhase) begin
        // Keep it set until we finish the cycle
        AddressPhase <= AddressPhase && ~(state == ALMOST_READY2);
      end
      else begin
        // set the address phase when ever we finish the address phase
        AddressPhase <= (state == ADDR_PHASE) && (bit_cnt == 3'b111);
      end
    end
  end

   /*********************************************
    *   DRP Signals
    *********************************************/
   always @ (posedge DRP_CLK) begin
      DRP_ADD     <= (nextstate == ADDR_PHASE);
      DRP_CS      <= (nextstate == ADDR_PHASE) | (nextstate == DATA_PHASE);
//      DRP_CS      <= (drp_ioi_addr != IOI_DQ0) ? (nextstate == ADDR_PHASE) | (nextstate == DATA_PHASE) : (bit_cnt != 3'b111) && (nextstate == ADDR_PHASE) | (nextstate == DATA_PHASE);
      MCB_UIREAD  <= (nextstate == DATA_PHASE) && rd_not_write_reg;
      if (state == READY)
        DRP_BKST  <= use_broadcast;
   end

   assign DRP_SDI_pre = (DRP_CS)? shift_through_reg[0] : 1'b0;  //if DRP_CS is inactive, just drive 0 out - this is a possible place to pipeline for increased performance
   assign DRP_SDI = (rd_not_write_reg & DRP_CS & !DRP_ADD)? DRP_SDO : DRP_SDI_pre; //If reading, then feed SDI back out SDO - this is a possible place to pipeline for increased performance


   /*********************************************
    *   State Machine
    *********************************************/
  always @ (*) begin
    addr_data_sel_n = 1'b0;
    load_shift_n = 1'b0;
    case (state)
      READY:  begin
         load_shift_n = 0;
         if(cmd_valid)
          nextstate = DECIDE;
         else
          nextstate = READY;
        end
      DECIDE: begin
          load_shift_n = 1;
          addr_data_sel_n = 1;
          nextstate = ADDR_PHASE;
        end
      ADDR_PHASE: begin
         load_shift_n = 0;
         if(&bit_cnt[2:0])
           if (`ALTERNATE_READ && rd_not_write_reg)
             if (AddressPhase)
               // After the second pass go to end of statemachine
               nextstate = ALMOST_READY;
             else
               // execute a second address phase for the alternative access method.
               nextstate = DECIDE;
           else
            nextstate = ADDR_TO_DATA_GAP;
         else
          nextstate = ADDR_PHASE;
        end
      ADDR_TO_DATA_GAP: begin
          load_shift_n = 1;
          nextstate = ADDR_TO_DATA_GAP2;
        end
      ADDR_TO_DATA_GAP2: begin
         load_shift_n = 1;
         nextstate = ADDR_TO_DATA_GAP3;
        end
      ADDR_TO_DATA_GAP3: begin
         load_shift_n = 1;
         nextstate = DATA_PHASE;
        end
      DATA_PHASE: begin
         load_shift_n = 0;
         if(&bit_cnt)
            nextstate = ALMOST_READY;
         else
          nextstate = DATA_PHASE;
        end
      ALMOST_READY: begin
         load_shift_n = 0;
         nextstate = ALMOST_READY2;
         end
      ALMOST_READY2: begin
         load_shift_n = 0;
         nextstate = ALMOST_READY3;
         end
      ALMOST_READY3: begin
         load_shift_n = 0;
         nextstate = READY;
         end
      default: begin
         load_shift_n = 0;
         nextstate = READY;
       end
    endcase
  end

  always @ (posedge DRP_CLK) begin
    if(sync_rst)
      state <= READY;
    else
      state <= nextstate;
   end

endmodule

module mcb_soft_calibration # (
  parameter       C_MEM_TZQINIT_MAXCNT  = 10'd512,  // DDR3 Minimum delay between resets
  parameter       C_MC_CALIBRATION_MODE = "CALIBRATION", // if set to CALIBRATION will reset DQS IDELAY to DQS_NUMERATOR/DQS_DENOMINATOR local_param values
                                                         // if set to NOCALIBRATION then defaults to hard cal blocks setting of C_MC_CALBRATION_DELAY (Quarter, etc)
  parameter       C_SIMULATION          = "FALSE",  // Tells us whether the design is being simulated or implemented
  parameter       SKIP_IN_TERM_CAL      = 1'b0,     // provides option to skip the input termination calibration
  parameter       SKIP_DYNAMIC_CAL      = 1'b0,     // provides option to skip the dynamic delay calibration
  parameter       SKIP_DYN_IN_TERM      = 1'b1,      // provides option to skip the input termination calibration
  parameter       C_MEM_TYPE = "DDR"            // provides the memory device used for the design
  
  )
  (
  input   wire            UI_CLK,                   // main clock input for logic and IODRP CLK pins.  At top level, this should also connect to IODRP2_MCB CLK pins
  input   wire            RST,                      // main system reset for both this Soft Calibration block - also will act as a passthrough to MCB's SYSRST
  (* IOB = "FALSE" *) output  reg            DONE_SOFTANDHARD_CAL,
                                                    // active high flag signals soft calibration of input delays is complete and MCB_UODONECAL is high (MCB hard calib complete)
  input   wire            PLL_LOCK,                 // Lock signal from PLL
  input   wire            SELFREFRESH_REQ,     
  input   wire            SELFREFRESH_MCB_MODE,
  output  reg             SELFREFRESH_MCB_REQ ,
  output  reg             SELFREFRESH_MODE,    
  output  wire            IODRP_ADD,                // IODRP ADD port
  output  wire            IODRP_SDI,                // IODRP SDI port
  input   wire            RZQ_IN,                   // RZQ pin from board - expected to have a 2*R resistor to ground
  input   wire            RZQ_IODRP_SDO,            // RZQ IODRP's SDO port
  output  reg             RZQ_IODRP_CS      = 1'b0, // RZQ IODRP's CS port
  input   wire            ZIO_IN,                   // Z-stated IO pin - garanteed not to be driven externally
  input   wire            ZIO_IODRP_SDO,            // ZIO IODRP's SDO port
  output  reg             ZIO_IODRP_CS      = 1'b0, // ZIO IODRP's CS port
  output  wire            MCB_UIADD,                // to MCB's UIADD port
  output  wire            MCB_UISDI,                // to MCB's UISDI port
  input   wire            MCB_UOSDO,                // from MCB's UOSDO port (User output SDO)
  input   wire            MCB_UODONECAL,            // indicates when MCB hard calibration process is complete
  input   wire            MCB_UOREFRSHFLAG,         //  high during refresh cycle and time when MCB is innactive
  output  wire            MCB_UICS,                 // to MCB's UICS port (User Input CS)
  output  reg             MCB_UIDRPUPDATE   = 1'b1, // MCB's UIDRPUPDATE port (gets passed to IODRP2_MCB's MEMUPDATE port: this controls shadow latch used during IODRP2_MCB writes).  Currently just trasnparent
  output  wire            MCB_UIBROADCAST,          // only to MCB's UIBROADCAST port (User Input BROADCAST - gets passed to IODRP2_MCB's BKST port)
  output  reg   [4:0]     MCB_UIADDR        = 5'b0, //  to MCB's UIADDR port (gets passed to IODRP2_MCB's AUXADDR port
  output  reg             MCB_UICMDEN       = 1'b1, //  set to 1 to take control of UI interface - removes control from internal calib block
  output  reg             MCB_UIDONECAL     = 1'b0, //  set to 0 to "tell" controller that it's still in a calibrate state
  output               MCB_UIDQLOWERDEC ,
  output               MCB_UIDQLOWERINC ,
  output               MCB_UIDQUPPERDEC ,
  output               MCB_UIDQUPPERINC ,
  output  reg             MCB_UILDQSDEC     = 1'b0,
  output  reg             MCB_UILDQSINC     = 1'b0,
  output  wire            MCB_UIREAD,               //  enables read w/o writing by turning on a SDO->SDI loopback inside the IODRP2_MCBs (doesn't exist in regular IODRP2).  IODRPCTRLR_R_WB becomes don't-care.
  output  reg             MCB_UIUDQSDEC     = 1'b0,
  output  reg             MCB_UIUDQSINC     = 1'b0,
  output                  MCB_RECAL         , //  future hook to drive MCB's RECAL pin - initiates a hard re-calibration sequence when high
  output  reg             MCB_UICMD,
  output  reg             MCB_UICMDIN,
  output  reg   [3:0]     MCB_UIDQCOUNT,
  input   wire  [7:0]     MCB_UODATA,
  input   wire            MCB_UODATAVALID,
  input   wire            MCB_UOCMDREADY,
  input   wire            MCB_UO_CAL_START,
  output  wire            MCB_SYSRST,               //  drives the MCB's SYSRST pin - the main reset for MCB
  output  reg   [7:0]     Max_Value,
  output  reg            CKE_Train
  );


localparam [4:0]
          IOI_DQ0       = {4'h0, 1'b1},
          IOI_DQ1       = {4'h0, 1'b0},
          IOI_DQ2       = {4'h1, 1'b1},
          IOI_DQ3       = {4'h1, 1'b0},
          IOI_DQ4       = {4'h2, 1'b1},
          IOI_DQ5       = {4'h2, 1'b0},
          IOI_DQ6       = {4'h3, 1'b1},
          IOI_DQ7       = {4'h3, 1'b0},
          IOI_DQ8       = {4'h4, 1'b1},
          IOI_DQ9       = {4'h4, 1'b0},
          IOI_DQ10      = {4'h5, 1'b1},
          IOI_DQ11      = {4'h5, 1'b0},
          IOI_DQ12      = {4'h6, 1'b1},
          IOI_DQ13      = {4'h6, 1'b0},
          IOI_DQ14      = {4'h7, 1'b1},
          IOI_DQ15      = {4'h7, 1'b0},
          IOI_UDM       = {4'h8, 1'b1},
          IOI_LDM       = {4'h8, 1'b0},
          IOI_CK_P      = {4'h9, 1'b1},
          IOI_CK_N      = {4'h9, 1'b0},
          IOI_RESET     = {4'hA, 1'b1},
          IOI_A11       = {4'hA, 1'b0},
          IOI_WE        = {4'hB, 1'b1},
          IOI_BA2       = {4'hB, 1'b0},
          IOI_BA0       = {4'hC, 1'b1},
          IOI_BA1       = {4'hC, 1'b0},
          IOI_RASN      = {4'hD, 1'b1},
          IOI_CASN      = {4'hD, 1'b0},
          IOI_UDQS_CLK  = {4'hE, 1'b1},
          IOI_UDQS_PIN  = {4'hE, 1'b0},
          IOI_LDQS_CLK  = {4'hF, 1'b1},
          IOI_LDQS_PIN  = {4'hF, 1'b0};

localparam  [5:0]   START                     = 6'h00,
                    LOAD_RZQ_NTERM            = 6'h01,
                    WAIT1                     = 6'h02,
                    LOAD_RZQ_PTERM            = 6'h03,
                    WAIT2                     = 6'h04,
                    INC_PTERM                 = 6'h05,
                    MULTIPLY_DIVIDE           = 6'h06,
                    LOAD_ZIO_PTERM            = 6'h07,
                    WAIT3                     = 6'h08,
                    LOAD_ZIO_NTERM            = 6'h09,
                    WAIT4                     = 6'h0A,
                    INC_NTERM                 = 6'h0B,
                    SKEW                      = 6'h0C,
                    WAIT_FOR_START_BROADCAST  = 6'h0D,
                    BROADCAST_PTERM           = 6'h0E,
                    WAIT5                     = 6'h0F,
                    BROADCAST_NTERM           = 6'h10,
                    WAIT6                     = 6'h11,
                    LDQS_CLK_WRITE_P_TERM     = 6'h12,
                    LDQS_CLK_P_TERM_WAIT      = 6'h13,
                    LDQS_CLK_WRITE_N_TERM     = 6'h14,
                    LDQS_CLK_N_TERM_WAIT      = 6'h15,
                    LDQS_PIN_WRITE_P_TERM     = 6'h16,
                    LDQS_PIN_P_TERM_WAIT      = 6'h17,
                    LDQS_PIN_WRITE_N_TERM     = 6'h18,
                    LDQS_PIN_N_TERM_WAIT      = 6'h19,
                    UDQS_CLK_WRITE_P_TERM     = 6'h1A,
                    UDQS_CLK_P_TERM_WAIT      = 6'h1B,
                    UDQS_CLK_WRITE_N_TERM     = 6'h1C,
                    UDQS_CLK_N_TERM_WAIT      = 6'h1D,
                    UDQS_PIN_WRITE_P_TERM     = 6'h1E,
                    UDQS_PIN_P_TERM_WAIT      = 6'h1F,
                    UDQS_PIN_WRITE_N_TERM     = 6'h20,
                    UDQS_PIN_N_TERM_WAIT      = 6'h21,
                    OFF_RZQ_PTERM             = 6'h22,
                    WAIT7                     = 6'h23,
                    OFF_ZIO_NTERM             = 6'h24,
                    WAIT8                     = 6'h25,
                    RST_DELAY                 = 6'h26,
                    START_DYN_CAL_PRE         = 6'h27,
                    WAIT_FOR_UODONE           = 6'h28,
                    LDQS_WRITE_POS_INDELAY    = 6'h29,
                    LDQS_WAIT1                = 6'h2A,
                    LDQS_WRITE_NEG_INDELAY    = 6'h2B,
                    LDQS_WAIT2                = 6'h2C,
                    UDQS_WRITE_POS_INDELAY    = 6'h2D,
                    UDQS_WAIT1                = 6'h2E,
                    UDQS_WRITE_NEG_INDELAY    = 6'h2F,
                    UDQS_WAIT2                = 6'h30,
                    START_DYN_CAL             = 6'h31,
                    WRITE_CALIBRATE           = 6'h32,
                    WAIT9                     = 6'h33,
                    READ_MAX_VALUE            = 6'h34,
                    WAIT10                    = 6'h35,
                    ANALYZE_MAX_VALUE         = 6'h36,
                    FIRST_DYN_CAL             = 6'h37,
                    INCREMENT                 = 6'h38,
                    DECREMENT                 = 6'h39,
                    DONE                      = 6'h3A;

localparam  [1:0]   RZQ           = 2'b00,
                    ZIO           = 2'b01,
                    MCB_PORT      = 2'b11;
localparam          WRITE_MODE    = 1'b0;
localparam          READ_MODE     = 1'b1;

// IOI Registers
localparam  [7:0]   NoOp          = 8'h00,
                    DelayControl  = 8'h01,
                    PosEdgeInDly  = 8'h02,
                    NegEdgeInDly  = 8'h03,
                    PosEdgeOutDly = 8'h04,
                    NegEdgeOutDly = 8'h05,
                    MiscCtl1      = 8'h06,
                    MiscCtl2      = 8'h07,
                    MaxValue      = 8'h08;

// IOB Registers
localparam  [7:0]   PDrive        = 8'h80,
                    PTerm         = 8'h81,
                    NDrive        = 8'h82,
                    NTerm         = 8'h83,
                    SlewRateCtl   = 8'h84,
                    LVDSControl   = 8'h85,
                    MiscControl   = 8'h86,
                    InputControl  = 8'h87,
                    TestReadback  = 8'h88;

// No multi/divide is required when a 55 ohm resister is used on RZQ
//localparam          MULT          = 1;
//localparam          DIV           = 1;
// use 7/4 scaling factor when the 100 ohm RZQ is used
localparam          MULT          = 7;
localparam          DIV           = 4;

localparam          PNSKEW        = 1'b1; //Default is 1'b1. Change to 1'b0 if PSKEW and NSKEW are not required
localparam          PNSKEWDQS     = 1'b1; 
localparam          MULT_S    = 9;
localparam          DIV_S     = 8;
localparam          MULT_W    = 7;
localparam          DIV_W     = 8;

localparam          DQS_NUMERATOR   = 3;
localparam          DQS_DENOMINATOR = 8;
localparam          INCDEC_THRESHOLD= 8'h03; // parameter for the threshold which triggers an inc/dec to occur.  2 for half, 4 for quarter, 3 for three eighths


                                                         
reg   [5:0]   P_Term       /* synthesis syn_preserve = 1 */;
reg   [6:0]   N_Term       /* synthesis syn_preserve = 1 */;
reg   [5:0]   P_Term_s     /* synthesis syn_preserve = 1 */;
reg   [6:0]   N_Term_s     /* synthesis syn_preserve = 1 */;
reg   [5:0]   P_Term_w     /* synthesis syn_preserve = 1 */;
reg   [6:0]   N_Term_w     /* synthesis syn_preserve = 1 */;
reg   [5:0]   P_Term_Prev  /* synthesis syn_preserve = 1 */;
reg   [6:0]   N_Term_Prev  /* synthesis syn_preserve = 1 */;
//(* FSM_ENCODING="USER" *) reg [5:0] STATE = START;   //XST does not pick up "BINARY" - use COMPACT instead if binary is desired
reg [5:0] STATE ;
reg   [7:0]   IODRPCTRLR_MEMCELL_ADDR /* synthesis syn_preserve = 1 */;
reg   [7:0]   IODRPCTRLR_WRITE_DATA /* synthesis syn_preserve = 1 */;
reg   [1:0]   Active_IODRP /* synthesis syn_maxfan = 1 */;
// synthesis attribute max_fanout of Active_IODRP is 1
reg           IODRPCTRLR_R_WB = 1'b0;
reg           IODRPCTRLR_CMD_VALID = 1'b0;
reg           IODRPCTRLR_USE_BKST = 1'b0;
reg           MCB_CMD_VALID = 1'b0;
reg           MCB_USE_BKST = 1'b0;
reg           Pre_SYSRST = 1'b1 /* synthesis syn_maxfan = 5 */; //internally generated reset which will OR with RST input to drive MCB's SYSRST pin (MCB_SYSRST)
// synthesis attribute max_fanout of Pre_SYSRST is 5
reg           IODRP_SDO;
reg   [7:0]   Max_Value_Previous  = 8'b0 /* synthesis syn_preserve = 1 */;
reg   [5:0]   count = 6'd0;               //counter for adding 18 extra clock cycles after setting Calibrate bit
reg           counter_en  = 1'b0;         //counter enable for "count"
reg           First_Dyn_Cal_Done = 1'b0;  //flag - high after the very first dynamic calibration is done
wire          START_BROADCAST ;     // Trigger to start Broadcast to IODRP2_MCBs to set Input Impedance - state machine will wait for this to be high
reg   [7:0]   DQS_DELAY_INITIAL   = 8'b0 /* synthesis syn_preserve = 1 */;
reg   [7:0]   DQS_DELAY ;        // contains the latest values written to LDQS and UDQS Input Delays
reg   [7:0]   TARGET_DQS_DELAY;  // used to track the target for DQS input delays - only gets updated if the Max Value changes by more than the threshold
reg   [7:0]   counter_inc;       // used to delay Inc signal by several ui_clk cycles (to deal with latency on UOREFRSHFLAG)
reg   [7:0]   counter_dec;       // used to delay Dec signal by several ui_clk cycles (to deal with latency on UOREFRSHFLAG)

wire  [7:0]   IODRPCTRLR_READ_DATA;
wire          IODRPCTRLR_RDY_BUSY_N;
wire          IODRP_CS;
wire  [7:0]   MCB_READ_DATA;

reg           RST_reg;
reg           Block_Reset;

reg           MCB_UODATAVALID_U;

wire  [2:0]   Inc_Dec_REFRSH_Flag;  // 3-bit flag to show:Inc is needed, Dec needed, refresh cycle taking place
wire  [7:0]   Max_Value_Delta_Up;   // tracks amount latest Max Value has gone up from previous Max Value read
wire  [7:0]   Half_MV_DU;           // half of Max_Value_Delta_Up
wire  [7:0]   Max_Value_Delta_Dn;   // tracks amount latest Max Value has gone down from previous Max Value read
wire  [7:0]   Half_MV_DD;           // half of Max_Value_Delta_Dn

reg   [9:0]   RstCounter = 10'h0;
wire          rst_tmp;
reg           LastPass_DynCal;
reg           First_In_Term_Done;
wire          Inc_Flag;               // flag to increment Dynamic Delay
wire          Dec_Flag;               // flag to decrement Dynamic Delay
                                                   
wire          CALMODE_EQ_CALIBRATION; // will calculate and set the DQS input delays if C_MC_CALIBRATION_MODE parameter = "CALIBRATION"
wire  [7:0]   DQS_DELAY_LOWER_LIMIT;  // Lower limit for DQS input delays 
wire  [7:0]   DQS_DELAY_UPPER_LIMIT;  // Upper limit for DQS input delays
wire          SKIP_DYN_IN_TERMINATION;//wire to allow skipping dynamic input termination if either the one-time or dynamic parameters are 1
wire          SKIP_DYNAMIC_DQS_CAL;   //wire allowing skipping dynamic DQS delay calibration if either SKIP_DYNIMIC_CAL=1, or if C_MC_CALIBRATION_MODE=NOCALIBRATION
wire  [7:0]   Quarter_Max_Value;
wire  [7:0]   Half_Max_Value;
reg           PLL_LOCK_R1;
reg           PLL_LOCK_R2;      

reg           SELFREFRESH_REQ_R1;
reg           SELFREFRESH_REQ_R2;
reg           SELFREFRESH_REQ_R3;
reg           SELFREFRESH_MCB_MODE_R1;
reg           SELFREFRESH_MCB_MODE_R2;
reg           SELFREFRESH_MCB_MODE_R3;

reg           WAIT_SELFREFRESH_EXIT_DQS_CAL;
reg           PERFORM_START_DYN_CAL_AFTER_SELFREFRESH;
reg           START_DYN_CAL_STATE_R1;
reg           PERFORM_START_DYN_CAL_AFTER_SELFREFRESH_R1;
reg           Rst_condition1;
wire          non_violating_rst;
reg [15:0]    WAIT_200us_COUNTER;
reg [7:0]     WaitTimer;
reg           WarmEnough;

wire   pre_sysrst_minpulse_width_ok;
reg [3:0] pre_sysrst_cnt;
// move the default assignment here to make FORMALITY happy.
assign START_BROADCAST = 1'b1;
assign MCB_RECAL = 1'b0;
assign MCB_UIDQLOWERDEC = 1'b0;
assign MCB_UIDQLOWERINC = 1'b0;
assign MCB_UIDQUPPERDEC = 1'b0;
assign MCB_UIDQUPPERINC = 1'b0;

// 'defines for which pass of the interleaved dynamic algorythm is taking place
`define IN_TERM_PASS  1'b0
`define DYN_CAL_PASS  1'b1

assign  Inc_Dec_REFRSH_Flag     = {Inc_Flag,Dec_Flag,MCB_UOREFRSHFLAG};
assign  Max_Value_Delta_Up      = Max_Value - Max_Value_Previous;
assign  Half_MV_DU              = {1'b0,Max_Value_Delta_Up[7:1]};
assign  Max_Value_Delta_Dn      = Max_Value_Previous - Max_Value;
assign  Half_MV_DD              = {1'b0,Max_Value_Delta_Dn[7:1]};
assign  CALMODE_EQ_CALIBRATION  = (C_MC_CALIBRATION_MODE == "CALIBRATION") ? 1'b1 : 1'b0; // will calculate and set the DQS input delays if = 1'b1
assign  Half_Max_Value          = Max_Value >> 1;
assign  Quarter_Max_Value       = Max_Value >> 2;
assign  DQS_DELAY_LOWER_LIMIT   = Quarter_Max_Value;  // limit for DQS_DELAY for decrements; could optionally be assigned to any 8-bit hex value here
assign  DQS_DELAY_UPPER_LIMIT   = Half_Max_Value;     // limit for DQS_DELAY for increments; could optionally be assigned to any 8-bit hex value here
assign  SKIP_DYN_IN_TERMINATION = SKIP_DYN_IN_TERM || SKIP_IN_TERM_CAL; //skip dynamic input termination if either the one-time or dynamic parameters are 1
assign  SKIP_DYNAMIC_DQS_CAL    = ~CALMODE_EQ_CALIBRATION || SKIP_DYNAMIC_CAL; //skip dynamic DQS delay calibration if either SKIP_DYNAMIC_CAL=1, or if C_MC_CALIBRATION_MODE=NOCALIBRATION

always @ (posedge UI_CLK)
     DONE_SOFTANDHARD_CAL    <= ((DQS_DELAY_INITIAL != 8'h00) || (STATE == DONE)) && MCB_UODONECAL;  //high when either DQS input delays initialized, or STATE=DONE and UODONECAL high


iodrp_controller iodrp_controller(
  .memcell_address  (IODRPCTRLR_MEMCELL_ADDR),
  .write_data       (IODRPCTRLR_WRITE_DATA),
  .read_data        (IODRPCTRLR_READ_DATA),
  .rd_not_write     (IODRPCTRLR_R_WB),
  .cmd_valid        (IODRPCTRLR_CMD_VALID),
  .rdy_busy_n       (IODRPCTRLR_RDY_BUSY_N),
  .use_broadcast    (1'b0),
  .sync_rst         (RST_reg),
  .DRP_CLK          (UI_CLK),
  .DRP_CS           (IODRP_CS),
  .DRP_SDI          (IODRP_SDI),
  .DRP_ADD          (IODRP_ADD),
  .DRP_SDO          (IODRP_SDO),
  .DRP_BKST         ()
  );

iodrp_mcb_controller iodrp_mcb_controller(
  .memcell_address  (IODRPCTRLR_MEMCELL_ADDR),
  .write_data       (IODRPCTRLR_WRITE_DATA),
  .read_data        (MCB_READ_DATA),
  .rd_not_write     (IODRPCTRLR_R_WB),
  .cmd_valid        (MCB_CMD_VALID),
  .rdy_busy_n       (MCB_RDY_BUSY_N),
  .use_broadcast    (MCB_USE_BKST),
  .drp_ioi_addr     (MCB_UIADDR),
  .sync_rst         (RST_reg),
  .DRP_CLK          (UI_CLK),
  .DRP_CS           (MCB_UICS),
  .DRP_SDI          (MCB_UISDI),
  .DRP_ADD          (MCB_UIADD),
  .DRP_BKST         (MCB_UIBROADCAST),
  .DRP_SDO          (MCB_UOSDO),
  .MCB_UIREAD       (MCB_UIREAD)
  );


//******************************************************************************************
// Mult_Divide Function - multiplies by a constant MULT and then divides by the DIV constant
//******************************************************************************************
function [7:0] Mult_Divide;
input   [7:0]   Input;
input   [7:0]   Mult;
input   [7:0]   Div;
reg     [3:0]   count;
reg     [15:0]   Result;
begin
  Result  = 0;
  for (count = 0; count < Mult; count = count+1) begin
    Result    = Result + Input;
  end
  Result      = Result / Div;
  Mult_Divide = Result[7:0];
end
endfunction

 always @ (posedge UI_CLK, posedge RST)
  begin
   if (RST)
     WAIT_200us_COUNTER <= (C_SIMULATION == "TRUE") ? 16'h7FF0 : 16'h0;
   else 
      if (WAIT_200us_COUNTER[15])  // UI_CLK maximum is up to 100 MHz.
        WAIT_200us_COUNTER <= WAIT_200us_COUNTER                        ;
      else
        WAIT_200us_COUNTER <= WAIT_200us_COUNTER + 1'b1;
  end 
    
    
generate
if( C_MEM_TYPE == "DDR2") begin : gen_cketrain_a


always @ ( posedge UI_CLK, posedge RST)
begin 
if (RST)
   CKE_Train <= 1'b0;
else 
  if (STATE == WAIT_FOR_UODONE && MCB_UODONECAL)
   CKE_Train <= 1'b0;
  else if (WAIT_200us_COUNTER[15] && ~MCB_UODONECAL)
   CKE_Train <= 1'b1;
  else
   CKE_Train <= 1'b0;
  
end
end
endgenerate


generate
if( C_MEM_TYPE != "DDR2") begin : gen_cketrain_b
always @ (RST)
   CKE_Train <= 1'b0;
end 
endgenerate

//********************************************
//PLL_LOCK and Reset signals
//********************************************
localparam  RST_CNT         = 10'h010;          //defines pulse-width for reset
localparam  TZQINIT_MAXCNT  = (C_MEM_TYPE == "DDR3") ? C_MEM_TZQINIT_MAXCNT + RST_CNT : 8 + RST_CNT;  
assign rst_tmp    = (~PLL_LOCK_R2 && ~SELFREFRESH_MODE); //rst_tmp becomes 1 if you lose PLL lock (registered twice for metastblty) and the device is not in SUSPEND

// Rst_contidtion1 is to make sure RESET will not happen again within TZQINIT_MAXCNT
assign non_violating_rst = RST & Rst_condition1;         //non_violating_rst is when the user-reset RST occurs and TZQINIT (min time between resets for DDR3) is not being violated


assign MCB_SYSRST = (Pre_SYSRST );

always @ (posedge UI_CLK or posedge RST ) begin  
  if (RST) begin         
    Block_Reset <= 1'b0;
    RstCounter  <= 10'b0;
end
  else begin
    Block_Reset <= 1'b0;                   //default to allow STATE to move out of RST_DELAY state
    if (Pre_SYSRST)
      RstCounter  <= RST_CNT;              //whenever STATE wants to reset the MCB, set RstCounter to h10
    else begin
      if (RstCounter < TZQINIT_MAXCNT) begin //if RstCounter is less than d512 than this will execute
        Block_Reset <= 1'b1;               //STATE won't exit RST_DELAY state
        RstCounter  <= RstCounter + 1'b1;  //and Rst_Counter increments
      end
    end
  end
end



always @ (posedge UI_CLK ) begin  
if (RstCounter >= TZQINIT_MAXCNT) 
    Rst_condition1 <= 1'b1;
else
    Rst_condition1 <= 1'b0;

end


// -- non_violating_rst asserts whenever (system-level reset) RST is asserted but must be after TZQINIT_MAXCNT is reached (min-time between resets for DDR3)
// -- After power stablizes, we will hold MCB in reset state for at least 200us before beginning initialization  process.   
// -- If the PLL loses lock during normal operation, no ui_clk will be present because mcb_drp_clk is from a BUFGCE which
//    is gated by pll's lock signal.   When the PLL locks again, the RST_reg stays asserted for at least 200 us which
//    will cause MCB to reset and reinitialize the memory afterwards.
// -- During SUSPEND operation, the PLL will lose lock but non_violating_rst remains low (de-asserted) and WAIT_200us_COUNTER stays at 
//    its terminal count.  The PLL_LOCK input does not come direct from PLL, rather it is driven by gated_pll_lock from mcb_raw_wrapper module
//    The gated_pll_lock in the mcb_raw_wrapper does not de-assert during SUSPEND operation, hence PLL_LOCK will not de-assert, and the soft calibration 
//    state machine will not reset during SUSPEND.
// -- RST_reg is the control signal that resets the mcb_soft_calibration's State Machine. The MCB_SYSRST is now equal to 
//    Pre_SYSRST. When State Machine is performing "INPUT Termination Calibration", it holds the MCB in reset by assertign MCB_SYSRST. 
//    It will deassert the MCB_SYSRST so that it can grab the bus to broadcast the P and N term value to all of the DQ pins. Once the calibrated INPUT 
//    termination is set, the State Machine will issue another short MCB_SYSRST so that MCB will use the tuned input termination during DQS preamble calibration.



always @ (posedge UI_CLK or posedge non_violating_rst ) begin  
  if (non_violating_rst)          
    RST_reg <= 1'b1;                                       
  else if (~WAIT_200us_COUNTER[15])
    RST_reg <= 1'b1;         
  else 
    RST_reg     <= rst_tmp; 
    
end


//********************************************
// stretching the pre_sysrst to satisfy the minimum pusle width

always @ (posedge UI_CLK )begin
  if (STATE == START_DYN_CAL_PRE)
     pre_sysrst_cnt <= pre_sysrst_cnt + 1;
  else
     pre_sysrst_cnt <= 4'b0;
end

assign pre_sysrst_minpulse_width_ok = pre_sysrst_cnt[3];

//********************************************
// SUSPEND Logic
//********************************************

always @ ( posedge UI_CLK, posedge RST) begin
  //SELFREFRESH_MCB_MODE is clocked by sysclk_2x_180
  if (RST)
    begin
      SELFREFRESH_MCB_MODE_R1 <= 1'b0;
      SELFREFRESH_MCB_MODE_R2 <= 1'b0;
      SELFREFRESH_MCB_MODE_R3 <= 1'b0;
      SELFREFRESH_REQ_R1      <= 1'b0;
      SELFREFRESH_REQ_R2      <= 1'b0;
      SELFREFRESH_REQ_R3      <= 1'b0;
      PLL_LOCK_R1             <= 1'b0;
      PLL_LOCK_R2             <= 1'b0;
    end
  else 
    begin
      SELFREFRESH_MCB_MODE_R1 <= SELFREFRESH_MCB_MODE;
      SELFREFRESH_MCB_MODE_R2 <= SELFREFRESH_MCB_MODE_R1;
      SELFREFRESH_MCB_MODE_R3 <= SELFREFRESH_MCB_MODE_R2;
      SELFREFRESH_REQ_R1      <= SELFREFRESH_REQ;
      SELFREFRESH_REQ_R2      <= SELFREFRESH_REQ_R1;
      SELFREFRESH_REQ_R3      <= SELFREFRESH_REQ_R2;
      PLL_LOCK_R1             <= PLL_LOCK;
      PLL_LOCK_R2             <= PLL_LOCK_R1;
    end
 end 

// SELFREFRESH should only be deasserted after PLL_LOCK is asserted.
// This is to make sure MCB get a locked sys_2x_clk before exiting
// SELFREFRESH mode.

always @ ( posedge UI_CLK) begin
  if (RST)
    SELFREFRESH_MCB_REQ <= 1'b0;
  else if (PLL_LOCK_R2 && ~SELFREFRESH_REQ_R3 )// 

    SELFREFRESH_MCB_REQ <=  1'b0;
  else if (STATE == START_DYN_CAL && SELFREFRESH_REQ_R3)  
    SELFREFRESH_MCB_REQ <= 1'b1;
end



always @ (posedge UI_CLK) begin
  if (RST)
    WAIT_SELFREFRESH_EXIT_DQS_CAL <= 1'b0;
  else if (~SELFREFRESH_MCB_MODE_R3 && SELFREFRESH_MCB_MODE_R2)  

    WAIT_SELFREFRESH_EXIT_DQS_CAL <= 1'b1;
  else if (WAIT_SELFREFRESH_EXIT_DQS_CAL && ~SELFREFRESH_REQ_R3 && PERFORM_START_DYN_CAL_AFTER_SELFREFRESH) // START_DYN_CAL is next state
    WAIT_SELFREFRESH_EXIT_DQS_CAL <= 1'b0;
end   

//Need to detect when SM entering START_DYN_CAL
always @ (posedge UI_CLK) begin
  if (RST) begin
    PERFORM_START_DYN_CAL_AFTER_SELFREFRESH  <= 1'b0;
    START_DYN_CAL_STATE_R1 <= 1'b0;
  end 
  else begin
    // register PERFORM_START_DYN_CAL_AFTER_SELFREFRESH to detect end of cycle
    PERFORM_START_DYN_CAL_AFTER_SELFREFRESH_R1 <= PERFORM_START_DYN_CAL_AFTER_SELFREFRESH;
    if (STATE == START_DYN_CAL)
      START_DYN_CAL_STATE_R1 <= 1'b1;
    else
      START_DYN_CAL_STATE_R1 <= 1'b0;
      if (WAIT_SELFREFRESH_EXIT_DQS_CAL && STATE != START_DYN_CAL && START_DYN_CAL_STATE_R1 )
        PERFORM_START_DYN_CAL_AFTER_SELFREFRESH <= 1'b1;
      else if (STATE == START_DYN_CAL && ~SELFREFRESH_MCB_MODE_R3)
        PERFORM_START_DYN_CAL_AFTER_SELFREFRESH <= 1'b0;
      end
  end
// SELFREFRESH_MCB_MODE deasserted status is hold off
// until Soft_Calib has at least done one loop of DQS update.
// New logic WarmeEnough is added to make sure PLL_Lock is lockec and all IOs stable before 
// deassert the status of MCB's SELFREFRESH_MODE.  This is to ensure all IOs are stable before
// user logic sending new commands to MCB.

always @ (posedge UI_CLK) begin
  if (RST)
    SELFREFRESH_MODE <= 1'b0;
  else if (SELFREFRESH_MCB_MODE_R2)  
    SELFREFRESH_MODE <= 1'b1;
    else if (WarmEnough)
     SELFREFRESH_MODE <= 1'b0;
end

reg WaitCountEnable;

always @ (posedge UI_CLK) begin
  if (RST)
    WaitCountEnable <= 1'b0;
  else if (~SELFREFRESH_REQ_R2 && SELFREFRESH_REQ_R1)  
    WaitCountEnable <= 1'b0;
    
  else if (!PERFORM_START_DYN_CAL_AFTER_SELFREFRESH && PERFORM_START_DYN_CAL_AFTER_SELFREFRESH_R1)
    WaitCountEnable <= 1'b1;
  else
    WaitCountEnable <=  WaitCountEnable;
end
reg State_Start_DynCal_R1 ;
reg State_Start_DynCal;
always @ (posedge UI_CLK)
begin
if (RST)
   State_Start_DynCal <= 1'b0;
else if (STATE == START_DYN_CAL)   
   State_Start_DynCal <= 1'b1;
else
   State_Start_DynCal <= 1'b0;
end

always @ (posedge UI_CLK)
begin
if (RST)
   State_Start_DynCal_R1 <= 1'b0;
else 
   State_Start_DynCal_R1 <= State_Start_DynCal;
end


always @ (posedge UI_CLK) begin
   if (RST) 
    begin
       WaitTimer <= 'b0;
       WarmEnough <= 1'b1;
    end       
  else if (~SELFREFRESH_REQ_R2 && SELFREFRESH_REQ_R1)  
    begin
       WaitTimer <= 'b0;
       WarmEnough <= 1'b0;
    end       
  else if (WaitTimer == 8'h4)
    begin
       WaitTimer <= WaitTimer ;
       WarmEnough <= 1'b1;
    end       
  else if (WaitCountEnable)
       WaitTimer <= WaitTimer + 1;
  else
       WaitTimer <= WaitTimer ;
  
end  



//********************************************
//Comparitors for Dynamic Calibration circuit
//********************************************
assign Dec_Flag = (TARGET_DQS_DELAY < DQS_DELAY);
assign Inc_Flag = (TARGET_DQS_DELAY > DQS_DELAY);


//*********************************************************************************************
//Counter for extra clock cycles injected after setting Calibrate bit in IODRP2 for Dynamic Cal
//*********************************************************************************************
 always @(posedge UI_CLK)
  begin
    if (RST_reg)
        count <= 6'd0;
    else if (counter_en)
        count <= count + 1'b1;
    else
        count <= 6'd0;
  end

//*********************************************************************************************
// Capture narrow MCB_UODATAVALID pulse - only one sysclk90 cycle wide
//*********************************************************************************************
 always @(posedge UI_CLK or posedge MCB_UODATAVALID)
  begin
    if (MCB_UODATAVALID)
        MCB_UODATAVALID_U <= 1'b1;
    else
        MCB_UODATAVALID_U <= MCB_UODATAVALID;
  end

  //**************************************************************************************************************
  //Always block to mux SDI, SDO, CS, and ADD depending on which IODRP is active: RZQ, ZIO or MCB's UI port (to IODRP2_MCBs)
  //**************************************************************************************************************
  always @(*) begin: ACTIVE_IODRP
    case (Active_IODRP)
      RZQ:      begin
        RZQ_IODRP_CS  = IODRP_CS;
        ZIO_IODRP_CS  = 1'b0;
        IODRP_SDO     = RZQ_IODRP_SDO;
      end
      ZIO:      begin
        RZQ_IODRP_CS  = 1'b0;
        ZIO_IODRP_CS  = IODRP_CS;
        IODRP_SDO     = ZIO_IODRP_SDO;
      end
      MCB_PORT: begin
        RZQ_IODRP_CS  = 1'b0;
        ZIO_IODRP_CS  = 1'b0;
        IODRP_SDO     = 1'b0;
      end
      default:  begin
        RZQ_IODRP_CS  = 1'b0;
        ZIO_IODRP_CS  = 1'b0;
        IODRP_SDO     = 1'b0;
      end
    endcase
  end

//******************************************************************
//State Machine's Always block / Case statement for Next State Logic
//
//The WAIT1,2,etc states were required after every state where the
//DRP controller was used to do a write to the IODRPs - this is because
//there's a clock cycle latency on IODRPCTRLR_RDY_BUSY_N whenever the DRP controller
//sees IODRPCTRLR_CMD_VALID go high.  OFF_RZQ_PTERM and OFF_ZIO_NTERM were added
//soley for the purpose of reducing power, particularly on RZQ as
//that pin is expected to have a permanent external resistor to gnd.
//******************************************************************
  always @(posedge UI_CLK) begin: NEXT_STATE_LOGIC
    if (RST_reg) begin                      // Synchronous reset
      MCB_CMD_VALID           <= 1'b0;
      MCB_UIADDR              <= 5'b0;
      MCB_UICMDEN             <= 1'b1;      // take control of UI/UO port
      MCB_UIDONECAL           <= 1'b0;      // tells MCB that it is in Soft Cal.
      MCB_USE_BKST            <= 1'b0;
      MCB_UIDRPUPDATE         <= 1'b1;
      Pre_SYSRST              <= 1'b1;      // keeps MCB in reset
      IODRPCTRLR_CMD_VALID    <= 1'b0;
      IODRPCTRLR_MEMCELL_ADDR <= NoOp;
      IODRPCTRLR_WRITE_DATA   <= 1'b0;
      IODRPCTRLR_R_WB         <= WRITE_MODE;
      IODRPCTRLR_USE_BKST     <= 1'b0;
      P_Term                  <= 6'b0;
      N_Term                  <= 7'b0;
      P_Term_s                <= 6'b0;
      N_Term_w                <= 7'b0;
      P_Term_w                <= 6'b0;
      N_Term_s                <= 7'b0;
      P_Term_Prev             <= 6'b0;
      N_Term_Prev             <= 7'b0;
      Active_IODRP            <= RZQ;
      MCB_UILDQSINC           <= 1'b0;      //no inc or dec
      MCB_UIUDQSINC           <= 1'b0;      //no inc or dec
      MCB_UILDQSDEC           <= 1'b0;      //no inc or dec
      MCB_UIUDQSDEC           <= 1'b0;      //no inc or dec
      counter_en              <= 1'b0;
      First_Dyn_Cal_Done      <= 1'b0;      //flag that the First Dynamic Calibration completed
      Max_Value               <= 8'b0;
      Max_Value_Previous      <= 8'b0;
      STATE                   <= START;
      DQS_DELAY               <= 8'h0; //tracks the cumulative incrementing/decrementing that has been done
      DQS_DELAY_INITIAL       <= 8'h0;
      TARGET_DQS_DELAY        <= 8'h0;
      LastPass_DynCal         <= `IN_TERM_PASS;
      First_In_Term_Done      <= 1'b0;
      MCB_UICMD               <= 1'b0;
      MCB_UICMDIN             <= 1'b0;
      MCB_UIDQCOUNT           <= 4'h0;
      counter_inc             <= 8'h0;
      counter_dec             <= 8'h0;
    end
    else begin
      counter_en              <= 1'b0;
      IODRPCTRLR_CMD_VALID    <= 1'b0;
      IODRPCTRLR_MEMCELL_ADDR <= NoOp;
      IODRPCTRLR_R_WB         <= READ_MODE;
      IODRPCTRLR_USE_BKST     <= 1'b0;
      MCB_CMD_VALID           <= 1'b0;
      MCB_UILDQSINC           <= 1'b0;            //no inc or dec
      MCB_UIUDQSINC           <= 1'b0;            //no inc or dec
      MCB_UILDQSDEC           <= 1'b0;            //no inc or dec
      MCB_UIUDQSDEC           <= 1'b0;            //no inc or dec
      MCB_USE_BKST            <= 1'b0;
      MCB_UICMDIN             <= 1'b0;
      DQS_DELAY               <= DQS_DELAY;
      TARGET_DQS_DELAY        <= TARGET_DQS_DELAY;
      case (STATE)
        START:  begin   //h00
          MCB_UICMDEN     <= 1'b1;        // take control of UI/UO port
          MCB_UIDONECAL   <= 1'b0;        // tells MCB that it is in Soft Cal.
          P_Term          <= 6'b0;
          N_Term          <= 7'b0;
          Pre_SYSRST      <= 1'b1;        // keeps MCB in reset
          LastPass_DynCal <= `IN_TERM_PASS;
          if (SKIP_IN_TERM_CAL) begin
               STATE <= WAIT_FOR_START_BROADCAST;
               P_Term <= 'b0;
               N_Term <= 'b0;
            end
          else if (IODRPCTRLR_RDY_BUSY_N)
            STATE  <= LOAD_RZQ_NTERM;
          else
            STATE  <= START;
        end
//***************************
// IOB INPUT TERMINATION CAL
//***************************
        LOAD_RZQ_NTERM: begin   //h01
          Active_IODRP            <= RZQ;
          IODRPCTRLR_CMD_VALID    <= 1'b1;
          IODRPCTRLR_MEMCELL_ADDR <= NTerm;
          IODRPCTRLR_WRITE_DATA   <= {1'b0,N_Term};
          IODRPCTRLR_R_WB         <= WRITE_MODE;
          if (IODRPCTRLR_RDY_BUSY_N)
            STATE <= LOAD_RZQ_NTERM;
          else
            STATE <= WAIT1;
        end
        WAIT1:  begin   //h02
          if (!IODRPCTRLR_RDY_BUSY_N)
            STATE <= WAIT1;
          else
            STATE <= LOAD_RZQ_PTERM;
        end
        LOAD_RZQ_PTERM: begin //h03
          IODRPCTRLR_CMD_VALID    <= 1'b1;
          IODRPCTRLR_MEMCELL_ADDR <= PTerm;
          IODRPCTRLR_WRITE_DATA   <= {2'b00,P_Term};
          IODRPCTRLR_R_WB         <= WRITE_MODE;
          if (IODRPCTRLR_RDY_BUSY_N)
            STATE <= LOAD_RZQ_PTERM;
          else
            STATE <= WAIT2;
        end
        WAIT2:  begin   //h04
          if (!IODRPCTRLR_RDY_BUSY_N)
            STATE <= WAIT2;
          else if ((RZQ_IN)||(P_Term == 6'b111111)) begin
            STATE <= MULTIPLY_DIVIDE;//LOAD_ZIO_PTERM;
          end
          else
            STATE <= INC_PTERM;
        end
        INC_PTERM: begin    //h05
          P_Term  <= P_Term + 1;
          STATE   <= LOAD_RZQ_PTERM;
        end
        MULTIPLY_DIVIDE: begin //06
           P_Term  <= Mult_Divide(P_Term-1, MULT, DIV);  //4/13/2011 compensate the added sync FF
           STATE <= LOAD_ZIO_PTERM;
        end
        LOAD_ZIO_PTERM: begin   //h07
          Active_IODRP            <= ZIO;
          IODRPCTRLR_CMD_VALID    <= 1'b1;
          IODRPCTRLR_MEMCELL_ADDR <= PTerm;
          IODRPCTRLR_WRITE_DATA   <= {2'b00,P_Term};
          IODRPCTRLR_R_WB         <= WRITE_MODE;
          if (IODRPCTRLR_RDY_BUSY_N)
            STATE <= LOAD_ZIO_PTERM;
          else
            STATE <= WAIT3;
        end
        WAIT3:  begin   //h08
          if (!IODRPCTRLR_RDY_BUSY_N)
            STATE <= WAIT3;
          else begin
            STATE   <= LOAD_ZIO_NTERM;
          end
        end
        LOAD_ZIO_NTERM: begin   //h09
          Active_IODRP            <= ZIO;
          IODRPCTRLR_CMD_VALID    <= 1'b1;
          IODRPCTRLR_MEMCELL_ADDR <= NTerm;
          IODRPCTRLR_WRITE_DATA   <= {1'b0,N_Term};
          IODRPCTRLR_R_WB         <= WRITE_MODE;
          if (IODRPCTRLR_RDY_BUSY_N)
            STATE <= LOAD_ZIO_NTERM;
          else
            STATE <= WAIT4;
        end
        WAIT4:  begin   //h0A
          if (!IODRPCTRLR_RDY_BUSY_N)
            STATE <= WAIT4;
          else if ((!ZIO_IN)||(N_Term == 7'b1111111)) begin
            if (PNSKEW) begin
              STATE    <= SKEW;
            end
            else 
            STATE <= WAIT_FOR_START_BROADCAST;
          end
          else
            STATE <= INC_NTERM;
        end
        INC_NTERM: begin    //h0B
          N_Term  <= N_Term + 1;
          STATE   <= LOAD_ZIO_NTERM;
        end
        SKEW : begin //0C
            P_Term_s <= Mult_Divide(P_Term, MULT_S, DIV_S);
            N_Term_w <= Mult_Divide(N_Term-1, MULT_W, DIV_W);
            P_Term_w <= Mult_Divide(P_Term, MULT_W, DIV_W);
            N_Term_s <= Mult_Divide(N_Term-1, MULT_S, DIV_S);
            P_Term   <= Mult_Divide(P_Term, MULT_S, DIV_S);
            N_Term   <= Mult_Divide(N_Term-1, MULT_W, DIV_W);
            STATE  <= WAIT_FOR_START_BROADCAST;
        end
        WAIT_FOR_START_BROADCAST: begin   //h0D
          Pre_SYSRST    <= 1'b0;      //release SYSRST, but keep UICMDEN=1 and UIDONECAL=0. This is needed to do Broadcast through UI interface, while keeping the MCB in calibration mode
          Active_IODRP  <= MCB_PORT;
          if (START_BROADCAST && IODRPCTRLR_RDY_BUSY_N) begin
            if (P_Term != P_Term_Prev || SKIP_IN_TERM_CAL   ) begin
              STATE       <= BROADCAST_PTERM;
              P_Term_Prev <= P_Term;
            end
            else if (N_Term != N_Term_Prev) begin
              N_Term_Prev <= N_Term;
              STATE       <= BROADCAST_NTERM;
            end
            else
              STATE <= OFF_RZQ_PTERM;
          end
          else
            STATE   <= WAIT_FOR_START_BROADCAST;
        end
        BROADCAST_PTERM:  begin    //h0E
//SBS redundant?          MCB_UICMDEN             <= 1'b1;        // take control of UI/UO port for reentrant use of dynamic In Term tuning
          IODRPCTRLR_MEMCELL_ADDR <= PTerm;
          IODRPCTRLR_WRITE_DATA   <= {2'b00,P_Term};
          IODRPCTRLR_R_WB         <= WRITE_MODE;
          MCB_CMD_VALID           <= 1'b1;
          MCB_UIDRPUPDATE         <= ~First_In_Term_Done; // Set the update flag if this is the first time through
          MCB_USE_BKST            <= 1'b1;
          if (MCB_RDY_BUSY_N)
            STATE <= BROADCAST_PTERM;
          else
            STATE <= WAIT5;
        end
        WAIT5:  begin   //h0F
          if (!MCB_RDY_BUSY_N)
            STATE <= WAIT5;
          else if (First_In_Term_Done) begin  // If first time through is already set, then this must be dynamic in term
            if (MCB_UOREFRSHFLAG) begin
              MCB_UIDRPUPDATE <= 1'b1;
              if (N_Term != N_Term_Prev) begin
                N_Term_Prev <= N_Term;
                STATE       <= BROADCAST_NTERM;
              end
              else
                STATE <= OFF_RZQ_PTERM;
            end
            else
              STATE <= WAIT5;   // wait for a Refresh cycle
          end
          else begin
            N_Term_Prev <= N_Term;
            STATE <= BROADCAST_NTERM;
          end
        end
        BROADCAST_NTERM:  begin    //h10
          IODRPCTRLR_MEMCELL_ADDR <= NTerm;
          IODRPCTRLR_WRITE_DATA   <= {2'b00,N_Term};
          IODRPCTRLR_R_WB         <= WRITE_MODE;
          MCB_CMD_VALID           <= 1'b1;
          MCB_USE_BKST            <= 1'b1;
          MCB_UIDRPUPDATE         <= ~First_In_Term_Done; // Set the update flag if this is the first time through
          if (MCB_RDY_BUSY_N)
            STATE <= BROADCAST_NTERM;
          else
            STATE <= WAIT6;
        end
        WAIT6:  begin             // 7'h11
          if (!MCB_RDY_BUSY_N)
            STATE <= WAIT6;
          else if (First_In_Term_Done) begin  // If first time through is already set, then this must be dynamic in term
            if (MCB_UOREFRSHFLAG) begin
              MCB_UIDRPUPDATE <= 1'b1;
              STATE           <= OFF_RZQ_PTERM;
            end
            else
              STATE <= WAIT6;   // wait for a Refresh cycle
          end
          else
               STATE <= LDQS_CLK_WRITE_P_TERM;
        end
          LDQS_CLK_WRITE_P_TERM:  begin   //7'h12
          IODRPCTRLR_MEMCELL_ADDR <= PTerm;
          IODRPCTRLR_R_WB         <= WRITE_MODE;
          IODRPCTRLR_WRITE_DATA   <= {2'b00, P_Term_w};
          MCB_UIADDR              <= IOI_LDQS_CLK;
          MCB_CMD_VALID           <= 1'b1;
          if (MCB_RDY_BUSY_N)
            STATE <= LDQS_CLK_WRITE_P_TERM;
          else
            STATE <= LDQS_CLK_P_TERM_WAIT;
        end
        LDQS_CLK_P_TERM_WAIT:  begin     //7'h13  
          if (!MCB_RDY_BUSY_N)
            STATE <= LDQS_CLK_P_TERM_WAIT;
          else begin
            STATE           <= LDQS_CLK_WRITE_N_TERM;
          end
        end
        LDQS_CLK_WRITE_N_TERM:  begin   //7'h14
          IODRPCTRLR_MEMCELL_ADDR <= NTerm;
          IODRPCTRLR_R_WB         <= WRITE_MODE;
          IODRPCTRLR_WRITE_DATA   <= {1'b0, N_Term_s};
          MCB_UIADDR              <= IOI_LDQS_CLK;
          MCB_CMD_VALID           <= 1'b1;
          if (MCB_RDY_BUSY_N)
            STATE <= LDQS_CLK_WRITE_N_TERM;
          else
            STATE <= LDQS_CLK_N_TERM_WAIT;
        end
        LDQS_CLK_N_TERM_WAIT:  begin   //7'h15
          if (!MCB_RDY_BUSY_N)
            STATE <= LDQS_CLK_N_TERM_WAIT;
          else begin
            STATE           <= LDQS_PIN_WRITE_P_TERM;
          end
        end
         LDQS_PIN_WRITE_P_TERM:  begin //7'h16
          IODRPCTRLR_MEMCELL_ADDR <= PTerm;
          IODRPCTRLR_R_WB         <= WRITE_MODE;
          IODRPCTRLR_WRITE_DATA   <= {2'b00, P_Term_s};
          MCB_UIADDR              <= IOI_LDQS_PIN;
          MCB_CMD_VALID           <= 1'b1;
          if (MCB_RDY_BUSY_N)
            STATE <= LDQS_PIN_WRITE_P_TERM;
          else
            STATE <= LDQS_PIN_P_TERM_WAIT;
        end
        LDQS_PIN_P_TERM_WAIT:  begin   //7'h17
          if (!MCB_RDY_BUSY_N)
            STATE <= LDQS_PIN_P_TERM_WAIT;
          else begin
            STATE           <= LDQS_PIN_WRITE_N_TERM;
          end
        end
         LDQS_PIN_WRITE_N_TERM:  begin //7'h18
          IODRPCTRLR_MEMCELL_ADDR <= NTerm;
          IODRPCTRLR_R_WB         <= WRITE_MODE;
          IODRPCTRLR_WRITE_DATA   <= {1'b0, N_Term_w};
          MCB_UIADDR              <= IOI_LDQS_PIN;
          MCB_CMD_VALID           <= 1'b1;
          if (MCB_RDY_BUSY_N)
            STATE <= LDQS_PIN_WRITE_N_TERM;
          else
            STATE <= LDQS_PIN_N_TERM_WAIT;
        end
        LDQS_PIN_N_TERM_WAIT:  begin  //7'h19
          if (!MCB_RDY_BUSY_N)
            STATE <= LDQS_PIN_N_TERM_WAIT;
          else begin
            STATE           <= UDQS_CLK_WRITE_P_TERM;
          end
        end
        UDQS_CLK_WRITE_P_TERM:  begin //7'h1A
          IODRPCTRLR_MEMCELL_ADDR <= PTerm;
          IODRPCTRLR_R_WB         <= WRITE_MODE;
          IODRPCTRLR_WRITE_DATA   <= {2'b00, P_Term_w};
          MCB_UIADDR              <= IOI_UDQS_CLK;
          MCB_CMD_VALID           <= 1'b1;
          if (MCB_RDY_BUSY_N)
            STATE <= UDQS_CLK_WRITE_P_TERM;
          else
            STATE <= UDQS_CLK_P_TERM_WAIT;
        end
        UDQS_CLK_P_TERM_WAIT:  begin //7'h1B
          if (!MCB_RDY_BUSY_N)
            STATE <= UDQS_CLK_P_TERM_WAIT;
          else begin
            STATE           <= UDQS_CLK_WRITE_N_TERM;
          end
        end
        UDQS_CLK_WRITE_N_TERM:  begin //7'h1C
          IODRPCTRLR_MEMCELL_ADDR <= NTerm;
          IODRPCTRLR_R_WB         <= WRITE_MODE;
          IODRPCTRLR_WRITE_DATA   <= {1'b0, N_Term_s};
          MCB_UIADDR              <= IOI_UDQS_CLK;
          MCB_CMD_VALID           <= 1'b1;
          if (MCB_RDY_BUSY_N)
            STATE <= UDQS_CLK_WRITE_N_TERM;
          else
            STATE <= UDQS_CLK_N_TERM_WAIT;
        end
        UDQS_CLK_N_TERM_WAIT:  begin //7'h1D
          if (!MCB_RDY_BUSY_N)
            STATE <= UDQS_CLK_N_TERM_WAIT;
          else begin
            STATE           <= UDQS_PIN_WRITE_P_TERM;
          end
        end
         UDQS_PIN_WRITE_P_TERM:  begin //7'h1E
          IODRPCTRLR_MEMCELL_ADDR <= PTerm;
          IODRPCTRLR_R_WB         <= WRITE_MODE;
          IODRPCTRLR_WRITE_DATA   <= {2'b00, P_Term_s};
          MCB_UIADDR              <= IOI_UDQS_PIN;
          MCB_CMD_VALID           <= 1'b1;
          if (MCB_RDY_BUSY_N)
            STATE <= UDQS_PIN_WRITE_P_TERM;
          else
            STATE <= UDQS_PIN_P_TERM_WAIT;
        end
        UDQS_PIN_P_TERM_WAIT:  begin  //7'h1F
          if (!MCB_RDY_BUSY_N)
            STATE <= UDQS_PIN_P_TERM_WAIT;
          else begin
            STATE           <= UDQS_PIN_WRITE_N_TERM;
          end
        end
         UDQS_PIN_WRITE_N_TERM:  begin  //7'h20
          IODRPCTRLR_MEMCELL_ADDR <= NTerm;
          IODRPCTRLR_R_WB         <= WRITE_MODE;
          IODRPCTRLR_WRITE_DATA   <= {1'b0, N_Term_w};
          MCB_UIADDR              <= IOI_UDQS_PIN;
          MCB_CMD_VALID           <= 1'b1;
          if (MCB_RDY_BUSY_N)
            STATE <= UDQS_PIN_WRITE_N_TERM;
          else
            STATE <= UDQS_PIN_N_TERM_WAIT;
        end
        UDQS_PIN_N_TERM_WAIT:  begin   //7'h21
          if (!MCB_RDY_BUSY_N)
            STATE <= UDQS_PIN_N_TERM_WAIT;
          else begin
            STATE           <= OFF_RZQ_PTERM;
          end
        end
        OFF_RZQ_PTERM:  begin        // 7'h22
          Active_IODRP            <= RZQ;
          IODRPCTRLR_CMD_VALID    <= 1'b1;
          IODRPCTRLR_MEMCELL_ADDR <= PTerm;
          IODRPCTRLR_WRITE_DATA   <= 8'b00;
          IODRPCTRLR_R_WB         <= WRITE_MODE;
          P_Term                  <= 6'b0;
          N_Term                  <= 5'b0;
          MCB_UIDRPUPDATE         <= ~First_In_Term_Done; // Set the update flag if this is the first time through
          if (IODRPCTRLR_RDY_BUSY_N)
            STATE <= OFF_RZQ_PTERM;
          else
            STATE <= WAIT7;
        end
        WAIT7:  begin             // 7'h23
          if (!IODRPCTRLR_RDY_BUSY_N)
            STATE <= WAIT7;
          else
            STATE <= OFF_ZIO_NTERM;
        end
        OFF_ZIO_NTERM:  begin     // 7'h24
          Active_IODRP            <= ZIO;
          IODRPCTRLR_CMD_VALID    <= 1'b1;
          IODRPCTRLR_MEMCELL_ADDR <= NTerm;
          IODRPCTRLR_WRITE_DATA   <= 8'b00;
          IODRPCTRLR_R_WB         <= WRITE_MODE;
          if (IODRPCTRLR_RDY_BUSY_N)
            STATE <= OFF_ZIO_NTERM;
          else
            STATE <= WAIT8;
        end
        WAIT8:  begin             // 7'h25
          if (!IODRPCTRLR_RDY_BUSY_N)
            STATE <= WAIT8;
          else begin
            if (First_In_Term_Done) begin
              STATE               <= START_DYN_CAL; // No need to reset the MCB if we are in InTerm tuning
            end
            else begin
              STATE               <= WRITE_CALIBRATE; // go read the first Max_Value from RZQ
            end
          end
        end
        RST_DELAY:  begin     // 7'h26
          if (Block_Reset) begin  // this ensures that more than 512 clock cycles occur since the last reset after MCB_WRITE_CALIBRATE ???
            STATE       <= RST_DELAY;
          end			 
          else begin
            STATE <= START_DYN_CAL_PRE;
          end
        end
       
//****************************
// DYNAMIC CALIBRATION PORTION
//****************************
        START_DYN_CAL_PRE:  begin   // 7'h27
          LastPass_DynCal <= `IN_TERM_PASS;
          MCB_UICMDEN     <= 1'b0;    // release UICMDEN
          MCB_UIDONECAL   <= 1'b1;    // release UIDONECAL - MCB will now initialize.
          Pre_SYSRST      <= 1'b1;    // SYSRST pulse
          if (~CALMODE_EQ_CALIBRATION)      // if C_MC_CALIBRATION_MODE is set to NOCALIBRATION
            STATE       <= START_DYN_CAL;  // we'll skip setting the DQS delays manually
          else if (pre_sysrst_minpulse_width_ok)   
            STATE       <= WAIT_FOR_UODONE;
          end
        WAIT_FOR_UODONE:  begin  //7'h28
          Pre_SYSRST      <= 1'b0;    // SYSRST pulse
          if (IODRPCTRLR_RDY_BUSY_N && MCB_UODONECAL) begin //IODRP Controller needs to be ready, & MCB needs to be done with hard calibration
            MCB_UICMDEN <= 1'b1;    // grab UICMDEN
            DQS_DELAY_INITIAL <= Mult_Divide(Max_Value, DQS_NUMERATOR, DQS_DENOMINATOR);
            STATE       <= LDQS_WRITE_POS_INDELAY;
          end
          else
            STATE       <= WAIT_FOR_UODONE;
        end
        LDQS_WRITE_POS_INDELAY:  begin// 7'h29
          IODRPCTRLR_MEMCELL_ADDR <= PosEdgeInDly;
          IODRPCTRLR_R_WB         <= WRITE_MODE;
          IODRPCTRLR_WRITE_DATA   <= DQS_DELAY_INITIAL;
          MCB_UIADDR              <= IOI_LDQS_CLK;
          MCB_CMD_VALID           <= 1'b1;
          if (MCB_RDY_BUSY_N)
            STATE <= LDQS_WRITE_POS_INDELAY;
          else
            STATE <= LDQS_WAIT1;
        end
        LDQS_WAIT1:  begin           // 7'h2A
          if (!MCB_RDY_BUSY_N)
            STATE <= LDQS_WAIT1;
          else begin
            STATE           <= LDQS_WRITE_NEG_INDELAY;
          end
        end
        LDQS_WRITE_NEG_INDELAY:  begin// 7'h2B
          IODRPCTRLR_MEMCELL_ADDR <= NegEdgeInDly;
          IODRPCTRLR_R_WB         <= WRITE_MODE;
          IODRPCTRLR_WRITE_DATA   <= DQS_DELAY_INITIAL;
          MCB_UIADDR              <= IOI_LDQS_CLK;
          MCB_CMD_VALID           <= 1'b1;
          if (MCB_RDY_BUSY_N)
            STATE <= LDQS_WRITE_NEG_INDELAY;
          else
            STATE <= LDQS_WAIT2;
        end
        LDQS_WAIT2:  begin           // 7'h2C
          if (!MCB_RDY_BUSY_N)
            STATE <= LDQS_WAIT2;
          else begin
            STATE <= UDQS_WRITE_POS_INDELAY;
          end
        end
        UDQS_WRITE_POS_INDELAY:  begin// 7'h2D
          IODRPCTRLR_MEMCELL_ADDR <= PosEdgeInDly;
          IODRPCTRLR_R_WB         <= WRITE_MODE;
          IODRPCTRLR_WRITE_DATA   <= DQS_DELAY_INITIAL;
          MCB_UIADDR              <= IOI_UDQS_CLK;
          MCB_CMD_VALID           <= 1'b1;
          if (MCB_RDY_BUSY_N)
            STATE <= UDQS_WRITE_POS_INDELAY;
          else
            STATE <= UDQS_WAIT1;
        end
        UDQS_WAIT1:  begin           // 7'h2E
          if (!MCB_RDY_BUSY_N)
            STATE <= UDQS_WAIT1;
          else begin
            STATE           <= UDQS_WRITE_NEG_INDELAY;
          end
        end
        UDQS_WRITE_NEG_INDELAY:  begin// 7'h2F
          IODRPCTRLR_MEMCELL_ADDR <= NegEdgeInDly;
          IODRPCTRLR_R_WB         <= WRITE_MODE;
          IODRPCTRLR_WRITE_DATA   <= DQS_DELAY_INITIAL;
          MCB_UIADDR              <= IOI_UDQS_CLK;
          MCB_CMD_VALID           <= 1'b1;
          if (MCB_RDY_BUSY_N)
            STATE <= UDQS_WRITE_NEG_INDELAY;
          else
            STATE <= UDQS_WAIT2;
        end
        UDQS_WAIT2:  begin           // 7'h30
          if (!MCB_RDY_BUSY_N)
            STATE <= UDQS_WAIT2;
          else begin
            DQS_DELAY         <= DQS_DELAY_INITIAL;
            TARGET_DQS_DELAY  <= DQS_DELAY_INITIAL;
            STATE             <= START_DYN_CAL;
          end
        end
//**************************************************************************************
        START_DYN_CAL:  begin       // 7'h31
          Pre_SYSRST        <= 1'b0;      // SYSRST not driven
          counter_inc       <= 8'b0;
          counter_dec       <= 8'b0;
          if (SKIP_DYNAMIC_DQS_CAL & SKIP_DYN_IN_TERMINATION)
            STATE <= DONE;  //if we're skipping both dynamic algorythms, go directly to DONE
          else
          if (IODRPCTRLR_RDY_BUSY_N && MCB_UODONECAL && ~SELFREFRESH_REQ_R1 ) begin  //IODRP Controller needs to be ready, & MCB needs to be done with hard calibration

            // Alternate between Dynamic Input Termination and Dynamic Tuning routines
            if (~SKIP_DYN_IN_TERMINATION & (LastPass_DynCal == `DYN_CAL_PASS)) begin
              LastPass_DynCal <= `IN_TERM_PASS;
              STATE           <= LOAD_RZQ_NTERM;
            end
            else begin
              LastPass_DynCal <= `DYN_CAL_PASS;
              STATE           <= WRITE_CALIBRATE;
            end
          end
          else
            STATE     <= START_DYN_CAL;
        end
        WRITE_CALIBRATE:  begin   // 7'h32
          Pre_SYSRST              <= 1'b0; // SYSRST not driven
          IODRPCTRLR_CMD_VALID    <= 1'b1;
          IODRPCTRLR_MEMCELL_ADDR <= DelayControl;
          IODRPCTRLR_WRITE_DATA   <= 8'h20; // Set calibrate bit
          IODRPCTRLR_R_WB         <= WRITE_MODE;
          Active_IODRP            <= RZQ;
          if (IODRPCTRLR_RDY_BUSY_N)
            STATE <= WRITE_CALIBRATE;
          else
            STATE <= WAIT9;
        end
        WAIT9:  begin     // 7'h33
          counter_en  <= 1'b1;
          if (count < 6'd38)  //this adds approximately 22 extra clock cycles after WRITE_CALIBRATE
            STATE     <= WAIT9;
          else
            STATE     <= READ_MAX_VALUE;
        end
        READ_MAX_VALUE: begin     // 7'h34
          IODRPCTRLR_CMD_VALID    <= 1'b1;
          IODRPCTRLR_MEMCELL_ADDR <= MaxValue;
          IODRPCTRLR_R_WB         <= READ_MODE;
          Max_Value_Previous      <= Max_Value;
          if (IODRPCTRLR_RDY_BUSY_N)
            STATE <= READ_MAX_VALUE;
          else
            STATE <= WAIT10;
        end
        WAIT10:  begin    // 7'h35
          if (!IODRPCTRLR_RDY_BUSY_N)
            STATE <= WAIT10;
          else begin
            Max_Value           <= IODRPCTRLR_READ_DATA;  //record the Max_Value from the IODRP controller
            if (~First_In_Term_Done) begin
              STATE               <= RST_DELAY;
              First_In_Term_Done  <= 1'b1;
            end
            else
              STATE               <= ANALYZE_MAX_VALUE;
          end
        end
        ANALYZE_MAX_VALUE:  begin // 7'h36   only do a Inc or Dec during a REFRESH cycle.
          if (!First_Dyn_Cal_Done)
            STATE <= FIRST_DYN_CAL;
          else
            if ((Max_Value<Max_Value_Previous)&&(Max_Value_Delta_Dn>=INCDEC_THRESHOLD)) begin
              STATE <= DECREMENT;         //May need to Decrement
              TARGET_DQS_DELAY   <= Mult_Divide(Max_Value, DQS_NUMERATOR, DQS_DENOMINATOR);
            end
          else
            if ((Max_Value>Max_Value_Previous)&&(Max_Value_Delta_Up>=INCDEC_THRESHOLD)) begin
              STATE <= INCREMENT;         //May need to Increment
              TARGET_DQS_DELAY   <= Mult_Divide(Max_Value, DQS_NUMERATOR, DQS_DENOMINATOR);
            end
          else begin
            Max_Value           <= Max_Value_Previous;
            STATE <= START_DYN_CAL;
          end
        end
        FIRST_DYN_CAL:  begin // 7'h37
          First_Dyn_Cal_Done  <= 1'b1;          //set flag that the First Dynamic Calibration has been completed
          STATE               <= START_DYN_CAL;
        end
        INCREMENT: begin      // 7'h38
          STATE               <= START_DYN_CAL; // Default case: Inc is not high or no longer in REFRSH
          MCB_UILDQSINC       <= 1'b0;          // Default case: no inc or dec
          MCB_UIUDQSINC       <= 1'b0;          // Default case: no inc or dec
          MCB_UILDQSDEC       <= 1'b0;          // Default case: no inc or dec
          MCB_UIUDQSDEC       <= 1'b0;          // Default case: no inc or dec
          case (Inc_Dec_REFRSH_Flag)            // {Increment_Flag,Decrement_Flag,MCB_UOREFRSHFLAG},
            3'b101: begin
              counter_inc <= counter_inc + 1'b1;
                STATE               <= INCREMENT; //Increment is still high, still in REFRSH cycle
              if (DQS_DELAY < DQS_DELAY_UPPER_LIMIT && counter_inc >= 8'h04) begin //if not at the upper limit yet, and you've waited 4 clks, increment
                MCB_UILDQSINC       <= 1'b1;      //increment
                MCB_UIUDQSINC       <= 1'b1;      //increment
                DQS_DELAY           <= DQS_DELAY + 1'b1;
              end
            end
            3'b100: begin
              if (DQS_DELAY < DQS_DELAY_UPPER_LIMIT)
                STATE                <= INCREMENT; //Increment is still high, REFRESH ended - wait for next REFRESH
              end
            default:  
                STATE               <= START_DYN_CAL; // Default case
          endcase
        end
        DECREMENT: begin      // 7'h39
          STATE               <= START_DYN_CAL; // Default case: Dec is not high or no longer in REFRSH
          MCB_UILDQSINC       <= 1'b0;          // Default case: no inc or dec
          MCB_UIUDQSINC       <= 1'b0;          // Default case: no inc or dec
          MCB_UILDQSDEC       <= 1'b0;          // Default case: no inc or dec
          MCB_UIUDQSDEC       <= 1'b0;          // Default case: no inc or dec
          if (DQS_DELAY != 8'h00) begin
            case (Inc_Dec_REFRSH_Flag)            // {Increment_Flag,Decrement_Flag,MCB_UOREFRSHFLAG},
              3'b011: begin
                counter_dec <= counter_dec + 1'b1;
                  STATE               <= DECREMENT; // Decrement is still high, still in REFRESH cycle
                if (DQS_DELAY > DQS_DELAY_LOWER_LIMIT  && counter_dec >= 8'h04) begin //if not at the lower limit, and you've waited 4 clks, decrement
                  MCB_UILDQSDEC       <= 1'b1;      // decrement
                  MCB_UIUDQSDEC       <= 1'b1;      // decrement
                  DQS_DELAY           <= DQS_DELAY - 1'b1; //SBS
                end
              end
              3'b010: begin
                if (DQS_DELAY > DQS_DELAY_LOWER_LIMIT) //if not at the lower limit, decrement
                  STATE                 <= DECREMENT; //Decrement is still high, REFRESH ended - wait for next REFRESH
                end
              default: begin
                  STATE               <= START_DYN_CAL; // Default case
              end
            endcase
          end
        end
        DONE: begin           // 7'h3A
          Pre_SYSRST              <= 1'b0;    // SYSRST cleared
          MCB_UICMDEN             <= 1'b0;  // release UICMDEN
          STATE <= DONE;
        end
        default:        begin
          MCB_UICMDEN             <= 1'b0;  // release UICMDEN
          MCB_UIDONECAL           <= 1'b1;  // release UIDONECAL - MCB will now initialize.
          Pre_SYSRST              <= 1'b0;  // SYSRST not driven
          IODRPCTRLR_CMD_VALID    <= 1'b0;
          IODRPCTRLR_MEMCELL_ADDR <= 8'h00;
          IODRPCTRLR_WRITE_DATA   <= 8'h00;
          IODRPCTRLR_R_WB         <= 1'b0;
          IODRPCTRLR_USE_BKST     <= 1'b0;
          P_Term                  <= 6'b0;
          N_Term                  <= 5'b0;
          Active_IODRP            <= ZIO;
          Max_Value_Previous      <= 8'b0;
          MCB_UILDQSINC           <= 1'b0;  // no inc or dec
          MCB_UIUDQSINC           <= 1'b0;  // no inc or dec
          MCB_UILDQSDEC           <= 1'b0;  // no inc or dec
          MCB_UIUDQSDEC           <= 1'b0;  // no inc or dec
          counter_en              <= 1'b0;
          First_Dyn_Cal_Done      <= 1'b0;  // flag that the First Dynamic Calibration completed
          Max_Value               <= Max_Value;
          STATE                   <= START;
        end
      endcase
    end
  end

endmodule
module mcb_raw_wrapper #
 
 (

parameter  C_MEMCLK_PERIOD          = 2500,       // /Mem clk period (in ps)
parameter  C_PORT_ENABLE            = 6'b111111,    //  config1 : 6b'111111,  config2: 4'b1111. config3 : 3'b111, config4: 2'b11, config5 1'b1
                                                  //  C_PORT_ENABLE[5] => User port 5,  ...,C_PORT_ENABLE[0] => User port 0
// Should the C_MEM_ADDR_ORDER made available to user ??
parameter  C_MEM_ADDR_ORDER             = "BANK_ROW_COLUMN" , //RowBankCol//ADDR_ORDER_MC : 0: Bank Row Col 1: Row Bank Col. User Address mapping oreder


parameter C_USR_INTERFACE_MODE       = "NATIVE", // Option is "NATIVE", "AXI"
                                               // This should default to "NATIVE" and only AXI interface
                                               // can set to "AXI"
////////////////////////////////////////////////////////////////////////////////////////////////
//  The parameter belows are not exposed to non-embedded users.

// for now this arb_time_slot_x attributes will not exposed to user and will be generated from MIG tool 
// to translate the logical port to physical port. For advance user, translate the logical port
// to physical port before passing them to this wrapper.
// MIG need to save the user setting in project file.
parameter  C_ARB_NUM_TIME_SLOTS     = 12,                      // For advance mode, allow user to either choose 10 or 12
parameter  C_ARB_TIME_SLOT_0        = 18'o012345,               // Config 1: "B32_B32_X32_X32_X32_X32"
parameter  C_ARB_TIME_SLOT_1        = 18'o123450,               //            User port 0 --->MCB port 0,User port 1 --->MCB port 1 
parameter  C_ARB_TIME_SLOT_2        = 18'o234501,               //            User port 2 --->MCB port 2,User port 3 --->MCB port 3
parameter  C_ARB_TIME_SLOT_3        = 18'o345012,               //            User port 4 --->MCB port 4,User port 5 --->MCB port 5
parameter  C_ARB_TIME_SLOT_4        = 18'o450123,               // Config 2: "B32_B32_B32_B32"  
parameter  C_ARB_TIME_SLOT_5        = 18'o501234,             //            User port 0     --->  MCB port 0
parameter  C_ARB_TIME_SLOT_6        = 18'o012345,             //            User port 1     --->  MCB port 1
parameter  C_ARB_TIME_SLOT_7        = 18'o123450,             //            User port 2     --->  MCB port 2
parameter  C_ARB_TIME_SLOT_8        = 18'o234501,             //            User port 3     --->  MCB port 4
parameter  C_ARB_TIME_SLOT_9        = 18'o345012,             // Config 3: "B64_B32_B3"   
parameter  C_ARB_TIME_SLOT_10       = 18'o450123,             //            User port 0     --->  MCB port 0
parameter  C_ARB_TIME_SLOT_11       = 18'o501234,             //            User port 1     --->  MCB port 2
                                                               //            User port 2     --->  MCB port 4
                                                               // Config 4: "B64_B64"              
                                                               //            User port 0     --->  MCB port 0
                                                               //            User port 1     --->  MCB port 2
                                                               // Config 5  "B128"              
                                                               //            User port 0     --->  MCB port 0
parameter  C_PORT_CONFIG               =  "B128",     



// Memory Timings
parameter  C_MEM_TRAS              =   45000,            //CEIL (tRAS/tCK)
parameter  C_MEM_TRCD               =   12500,            //CEIL (tRCD/tCK)
parameter  C_MEM_TREFI              =   7800,             //CEIL (tREFI/tCK) number of clocks
parameter  C_MEM_TRFC               =   127500,           //CEIL (tRFC/tCK)
parameter  C_MEM_TRP                =   12500,            //CEIL (tRP/tCK)
parameter  C_MEM_TWR                =   15000,            //CEIL (tWR/tCK)
parameter  C_MEM_TRTP               =   7500,             //CEIL (tRTP/tCK)
parameter  C_MEM_TWTR               =   7500,

parameter  C_NUM_DQ_PINS               =  8,                   
parameter  C_MEM_TYPE                  =  "DDR3",  
parameter  C_MEM_DENSITY               =  "512M",
parameter  C_MEM_BURST_LEN             =  8,       // MIG Rules for setting this parameter
                                                   // For DDR3  this one always set to 8; 
                                                   // For DDR2  Config 1 : MemWidth x8,x16:=> 4; MemWidth  x4     => 8
                                                   //           Config 2 : MemWidth x8,x16:=> 4; MemWidth  x4     => 8
                                                   //           Config 3 : Data Port Width: 32   MemWidth x8,x16:=> 4; MemWidth  x4     => 8
                                                   //                      Data Port Width: 64   MemWidth x16   :=> 4; MemWidth  x8,x4     => 8
                                                   //           Config 4 : Data Port Width: 64   MemWidth x16   :=> 4; MemWidth  x4,x8, => 8    
                                                   //           Config 5 : Data Port Width: 128  MemWidth x4, x8,x16: => 8
                                                                                           
                                               
                                                              
parameter  C_MEM_CAS_LATENCY           =  4,
parameter  C_MEM_ADDR_WIDTH            =  13,    // extracted from selected Memory part
parameter  C_MEM_BANKADDR_WIDTH        =  3,     // extracted from selected Memory part
parameter  C_MEM_NUM_COL_BITS          =  11,    // extracted from selected Memory part

parameter  C_MEM_DDR3_CAS_LATENCY      = 7,   
parameter  C_MEM_MOBILE_PA_SR          = "FULL",  //"FULL", "HALF" Mobile DDR Partial Array Self-Refresh 
parameter  C_MEM_DDR1_2_ODS            = "FULL",  //"FULL"  :REDUCED" 
parameter  C_MEM_DDR3_ODS              = "DIV6",   
parameter  C_MEM_DDR2_RTT              = "50OHMS",    
parameter  C_MEM_DDR3_RTT              =  "DIV2",  
parameter  C_MEM_MDDR_ODS              =  "FULL",   

parameter  C_MEM_DDR2_DIFF_DQS_EN      =  "YES", 
parameter  C_MEM_DDR2_3_PA_SR          =  "OFF",  
parameter  C_MEM_DDR3_CAS_WR_LATENCY   =   5,        // this parameter is hardcoded  by MIG tool which depends on the memory clock frequency
                                                     //C_MEMCLK_PERIOD ave = 2.5ns to < 3.3 ns, CWL = 5 
                                                     //C_MEMCLK_PERIOD ave = 1.875ns to < 2.5 ns, CWL = 6 
                                                     //C_MEMCLK_PERIOD ave = 1.5ns to <1.875ns, CSL = 7 
                                                     //C_MEMCLK_PERIOD avg = 1.25ns to < 1.5ns , CWL = 8

parameter  C_MEM_DDR3_AUTO_SR         =  "ENABLED",
parameter  C_MEM_DDR2_3_HIGH_TEMP_SR  =  "NORMAL",
parameter  C_MEM_DDR3_DYN_WRT_ODT     =  "OFF",
parameter  C_MEM_TZQINIT_MAXCNT       = 10'd512,  // DDR3 Minimum delay between resets

//Calibration 
parameter  C_MC_CALIB_BYPASS        = "NO",
parameter  C_MC_CALIBRATION_RA      = 15'h0000,
parameter  C_MC_CALIBRATION_BA      = 3'h0,

parameter C_CALIB_SOFT_IP           = "TRUE",
parameter C_SKIP_IN_TERM_CAL = 1'b0,     //provides option to skip the input termination calibration
parameter C_SKIP_DYNAMIC_CAL = 1'b0,     //provides option to skip the dynamic delay calibration
parameter C_SKIP_DYN_IN_TERM = 1'b1,     // provides option to skip the input termination calibration
parameter C_SIMULATION       = "FALSE",  // Tells us whether the design is being simulated or implemented

////////////////LUMP DELAY Params ////////////////////////////
/// ADDED for 1.0 silicon support to bypass Calibration //////
/// 07-10-09 chipl
//////////////////////////////////////////////////////////////
parameter LDQSP_TAP_DELAY_VAL  = 0,  // 0 to 255 inclusive
parameter UDQSP_TAP_DELAY_VAL  = 0,  // 0 to 255 inclusive
parameter LDQSN_TAP_DELAY_VAL  = 0,  // 0 to 255 inclusive
parameter UDQSN_TAP_DELAY_VAL  = 0,  // 0 to 255 inclusive
parameter DQ0_TAP_DELAY_VAL  = 0,  // 0 to 255 inclusive
parameter DQ1_TAP_DELAY_VAL  = 0,  // 0 to 255 inclusive
parameter DQ2_TAP_DELAY_VAL  = 0,  // 0 to 255 inclusive
parameter DQ3_TAP_DELAY_VAL  = 0,  // 0 to 255 inclusive
parameter DQ4_TAP_DELAY_VAL  = 0,  // 0 to 255 inclusive
parameter DQ5_TAP_DELAY_VAL  = 0,  // 0 to 255 inclusive
parameter DQ6_TAP_DELAY_VAL  = 0,  // 0 to 255 inclusive
parameter DQ7_TAP_DELAY_VAL  = 0,  // 0 to 255 inclusive
parameter DQ8_TAP_DELAY_VAL  = 0,  // 0 to 255 inclusive
parameter DQ9_TAP_DELAY_VAL  = 0,  // 0 to 255 inclusive
parameter DQ10_TAP_DELAY_VAL = 0,  // 0 to 255 inclusive
parameter DQ11_TAP_DELAY_VAL = 0,  // 0 to 255 inclusive
parameter DQ12_TAP_DELAY_VAL = 0,  // 0 to 255 inclusive
parameter DQ13_TAP_DELAY_VAL = 0,  // 0 to 255 inclusive
parameter DQ14_TAP_DELAY_VAL = 0,  // 0 to 255 inclusive
parameter DQ15_TAP_DELAY_VAL = 0,  // 0 to 255 inclusive
//*************
// MIG tool need to do DRC on this parameter to make sure this is valid Column address to avoid boundary crossing for the current Burst Size setting.
parameter  C_MC_CALIBRATION_CA      = 12'h000,
parameter  C_MC_CALIBRATION_CLK_DIV     = 1,
parameter  C_MC_CALIBRATION_MODE    = "CALIBRATION"     ,   // "CALIBRATION", "NOCALIBRATION"
parameter  C_MC_CALIBRATION_DELAY   = "HALF",   // "QUARTER", "HALF","THREEQUARTER", "FULL"

parameter C_P0_MASK_SIZE           = 4,
parameter C_P0_DATA_PORT_SIZE      = 32,
parameter C_P1_MASK_SIZE           = 4,
parameter C_P1_DATA_PORT_SIZE         = 32

    )
  (
  
      // high-speed PLL clock interface
      
      input sysclk_2x,                         
      input sysclk_2x_180,                      
      input pll_ce_0,
      input pll_ce_90,
      input pll_lock,                          
      input sys_rst,                         
      // Not needed as ioi netlist are not used
//***********************************************************************************
//  Below User Port siganls needs to be customized when generating codes from MIG tool
//  The corresponding internal codes that directly use the commented out port signals 
//  needs to be removed when gernerating wrapper outputs.
//!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!

      //User Port0 Interface Signals
      // p0_xxxx signals  shows up in Config 1 , Config 2 , Config 3, Config4 and Config 5
      // cmd port 0 signals

      input             p0_arb_en,
      input             p0_cmd_clk,
      input             p0_cmd_en,
      input [2:0]       p0_cmd_instr,
      input [5:0]       p0_cmd_bl,
      input [29:0]      p0_cmd_byte_addr,
      output            p0_cmd_empty,
      output            p0_cmd_full,

      // Data Wr Port signals
      // p0_wr_xx signals  shows up in Config 1 
      // p0_wr_xx signals  shows up in Config 2
      // p0_wr_xx signals  shows up in Config 3
      // p0_wr_xx signals  shows up in Config 4
      // p0_wr_xx signals  shows up in Config 5
      
      input             p0_wr_clk,
      input             p0_wr_en,
      input [C_P0_MASK_SIZE - 1:0]      p0_wr_mask,
      input [C_P0_DATA_PORT_SIZE - 1:0] p0_wr_data,
      output            p0_wr_full,        //
      output            p0_wr_empty,//
      output [6:0]      p0_wr_count,//
      output            p0_wr_underrun,//
      output            p0_wr_error,//

      //Data Rd Port signals
      // p0_rd_xx signals  shows up in Config 1 
      // p0_rd_xx signals  shows up in Config 2
      // p0_rd_xx signals  shows up in Config 3
      // p0_rd_xx signals  shows up in Config 4
      // p0_rd_xx signals  shows up in Config 5
      
      input             p0_rd_clk,
      input             p0_rd_en,
      output [C_P0_DATA_PORT_SIZE - 1:0]        p0_rd_data,
      output            p0_rd_full,//
      output            p0_rd_empty,//
      output [6:0]      p0_rd_count,
      output            p0_rd_overflow,//
      output            p0_rd_error,//

      
      //****************************
      //User Port1 Interface Signals
      // This group of signals only appear on Config 1,2,3,4 when generated from MIG tool

      input             p1_arb_en,
      input             p1_cmd_clk,
      input             p1_cmd_en,
      input [2:0]       p1_cmd_instr,
      input [5:0]       p1_cmd_bl,
      input [29:0]      p1_cmd_byte_addr,
      output            p1_cmd_empty,
      output            p1_cmd_full,

      // Data Wr Port signals
      input             p1_wr_clk,
      input             p1_wr_en,
      input [C_P1_MASK_SIZE - 1:0]      p1_wr_mask,
      input [C_P1_DATA_PORT_SIZE - 1:0] p1_wr_data,
      output            p1_wr_full,
      output            p1_wr_empty,
      output [6:0]      p1_wr_count,
      output            p1_wr_underrun,
      output            p1_wr_error,

      //Data Rd Port signals
      input             p1_rd_clk,
      input             p1_rd_en,
      output [C_P1_DATA_PORT_SIZE - 1:0]        p1_rd_data,
      output            p1_rd_full,
      output            p1_rd_empty,
      output [6:0]      p1_rd_count,
      output            p1_rd_overflow,
      output            p1_rd_error,

      
      //****************************
      //User Port2 Interface Signals
      // This group of signals only appear on Config 1,2,3 when generated from MIG tool
      // p2_xxxx signals  shows up in Config 1 , Config 2 and Config 3
      // p_cmd port 2 signals

      input             p2_arb_en,
      input             p2_cmd_clk,
      input             p2_cmd_en,
      input [2:0]       p2_cmd_instr,
      input [5:0]       p2_cmd_bl,
      input [29:0]      p2_cmd_byte_addr,
      output            p2_cmd_empty,
      output            p2_cmd_full,

      // Data Wr Port signals
      // p2_wr_xx signals  shows up in Config 1 and Wr Dir  
      // p2_wr_xx signals  shows up in Config 2
      // p2_wr_xx signals  shows up in Config 3
      
      input             p2_wr_clk,
      input             p2_wr_en,
      input [3:0]       p2_wr_mask,
      input [31:0]      p2_wr_data,
      output            p2_wr_full,
      output            p2_wr_empty,
      output [6:0]      p2_wr_count,
      output            p2_wr_underrun,
      output            p2_wr_error,

      //Data Rd Port signals
      // p2_rd_xx signals  shows up in Config 1 and Rd Dir
      // p2_rd_xx signals  shows up in Config 2
      // p2_rd_xx signals  shows up in Config 3
      
      input             p2_rd_clk,
      input             p2_rd_en,
      output [31:0]     p2_rd_data,
      output            p2_rd_full,
      output            p2_rd_empty,
      output [6:0]      p2_rd_count,
      output            p2_rd_overflow,
      output            p2_rd_error,

      
      //****************************
      //User Port3 Interface Signals
      // This group of signals only appear on Config 1,2 when generated from MIG tool

      input             p3_arb_en,
      input             p3_cmd_clk,
      input             p3_cmd_en,
      input [2:0]       p3_cmd_instr,
      input [5:0]       p3_cmd_bl,
      input [29:0]      p3_cmd_byte_addr,
      output            p3_cmd_empty,
      output            p3_cmd_full,

      // Data Wr Port signals
      // p3_wr_xx signals  shows up in Config 1 and Wr Dir
      // p3_wr_xx signals  shows up in Config 2
      
      input             p3_wr_clk,
      input             p3_wr_en,
      input [3:0]       p3_wr_mask,
      input [31:0]      p3_wr_data,
      output            p3_wr_full,
      output            p3_wr_empty,
      output [6:0]      p3_wr_count,
      output            p3_wr_underrun,
      output            p3_wr_error,

      //Data Rd Port signals
      // p3_rd_xx signals  shows up in Config 1 and Rd Dir when generated from MIG ttols
      // p3_rd_xx signals  shows up in Config 2 
      
      input             p3_rd_clk,
      input             p3_rd_en,
      output [31:0]     p3_rd_data,
      output            p3_rd_full,
      output            p3_rd_empty,
      output [6:0]      p3_rd_count,
      output            p3_rd_overflow,
      output            p3_rd_error,
      //****************************
      //User Port4 Interface Signals
      // This group of signals only appear on Config 1,2,3,4 when generated from MIG tool
      // p4_xxxx signals only shows up in Config 1

      input             p4_arb_en,
      input             p4_cmd_clk,
      input             p4_cmd_en,
      input [2:0]       p4_cmd_instr,
      input [5:0]       p4_cmd_bl,
      input [29:0]      p4_cmd_byte_addr,
      output            p4_cmd_empty,
      output            p4_cmd_full,

      // Data Wr Port signals
      // p4_wr_xx signals only shows up in Config 1 and Wr Dir
      
      input             p4_wr_clk,
      input             p4_wr_en,
      input [3:0]       p4_wr_mask,
      input [31:0]      p4_wr_data,
      output            p4_wr_full,
      output            p4_wr_empty,
      output [6:0]      p4_wr_count,
      output            p4_wr_underrun,
      output            p4_wr_error,

      //Data Rd Port signals
      // p4_rd_xx signals only shows up in Config 1 and Rd Dir
      
      input             p4_rd_clk,
      input             p4_rd_en,
      output [31:0]     p4_rd_data,
      output            p4_rd_full,
      output            p4_rd_empty,
      output [6:0]      p4_rd_count,
      output            p4_rd_overflow,
      output            p4_rd_error,


      //****************************
      //User Port5 Interface Signals
      // p5_xxxx signals only shows up in Config 1; p5_wr_xx or p5_rd_xx depends on the user port settings

      input             p5_arb_en,
      input             p5_cmd_clk,
      input             p5_cmd_en,
      input [2:0]       p5_cmd_instr,
      input [5:0]       p5_cmd_bl,
      input [29:0]      p5_cmd_byte_addr,
      output            p5_cmd_empty,
      output            p5_cmd_full,

      // Data Wr Port signals
      input             p5_wr_clk,
      input             p5_wr_en,
      input [3:0]       p5_wr_mask,
      input [31:0]      p5_wr_data,
      output            p5_wr_full,
      output            p5_wr_empty,
      output [6:0]      p5_wr_count,
      output            p5_wr_underrun,
      output            p5_wr_error,

      //Data Rd Port signals
      input             p5_rd_clk,
      input             p5_rd_en,
      output [31:0]     p5_rd_data,
      output            p5_rd_full,
      output            p5_rd_empty,
      output [6:0]      p5_rd_count,
      output            p5_rd_overflow,
      output            p5_rd_error,
      
//*****************************************************
      // memory interface signals    
      output [C_MEM_ADDR_WIDTH-1:0]     mcbx_dram_addr,  
      output [C_MEM_BANKADDR_WIDTH-1:0] mcbx_dram_ba,
      output                            mcbx_dram_ras_n,                        
      output                            mcbx_dram_cas_n,                        
      output                            mcbx_dram_we_n,                         
                                      
      output                            mcbx_dram_cke,                          
      output                            mcbx_dram_clk,                          
      output                            mcbx_dram_clk_n,                        
      inout [C_NUM_DQ_PINS-1:0]         mcbx_dram_dq,              
      inout                             mcbx_dram_dqs,                          
      inout                             mcbx_dram_dqs_n,                        
      inout                             mcbx_dram_udqs,                         
      inout                             mcbx_dram_udqs_n,                       
      
      output                            mcbx_dram_udm,                          
      output                            mcbx_dram_ldm,                          
      output                            mcbx_dram_odt,                          
      output                            mcbx_dram_ddr3_rst,                     
      // Calibration signals
      input calib_recal,              // Input signal to trigger calibration
     // output calib_done,        // 0=calibration not done or is in progress.  
                                // 1=calibration is complete.  Also a MEM_READY indicator
                                
   //Input - RZQ pin from board - expected to have a 2*R resistor to ground
   //Input - Z-stated IO pin - either unbonded IO, or IO garanteed not to be driven externally
                                
      inout                             rzq,           // RZQ pin from board - expected to have a 2*R resistor to ground
      inout                             zio,           // Z-stated IO pin - either unbonded IO, or IO garanteed not to be driven externally
      // new added signals *********************************
      // these signals are for dynamic Calibration IP
      input                             ui_read,
      input                             ui_add,
      input                             ui_cs,
      input                             ui_clk,
      input                             ui_sdi,
      input     [4:0]                   ui_addr,
      input                             ui_broadcast,
      input                             ui_drp_update,
      input                             ui_done_cal,
      input                             ui_cmd,
      input                             ui_cmd_in,
      input                             ui_cmd_en,
      input     [3:0]                   ui_dqcount,
      input                             ui_dq_lower_dec,
      input                             ui_dq_lower_inc,
      input                             ui_dq_upper_dec,
      input                             ui_dq_upper_inc,
      input                             ui_udqs_inc,
      input                             ui_udqs_dec,
      input                             ui_ldqs_inc,
      input                             ui_ldqs_dec,
      output     [7:0]                  uo_data,
      output                            uo_data_valid,
      output                            uo_done_cal,
      output                            uo_cmd_ready_in,
      output                            uo_refrsh_flag,
      output                            uo_cal_start,
      output                            uo_sdo,
      output   [31:0]                   status,
      input                             selfrefresh_enter,              
      output                            selfrefresh_mode
         );
  function integer cdiv (input integer num,
                         input integer div); // ceiling divide
    begin
      cdiv = (num/div) + (((num%div)>0) ? 1 : 0);
    end
  endfunction // cdiv

// parameters added by AM for OSERDES2 12/09/2008, these parameters may not have to change 
localparam C_OSERDES2_DATA_RATE_OQ = "SDR";           //SDR, DDR
localparam C_OSERDES2_DATA_RATE_OT = "SDR";           //SDR, DDR
localparam C_OSERDES2_SERDES_MODE_MASTER  = "MASTER";        //MASTER, SLAVE
localparam C_OSERDES2_SERDES_MODE_SLAVE   = "SLAVE";        //MASTER, SLAVE
localparam C_OSERDES2_OUTPUT_MODE_SE      = "SINGLE_ENDED";   //SINGLE_ENDED, DIFFERENTIAL
localparam C_OSERDES2_OUTPUT_MODE_DIFF    = "DIFFERENTIAL";
 
localparam C_BUFPLL_0_LOCK_SRC       = "LOCK_TO_0";

localparam C_DQ_IODRP2_DATA_RATE             = "SDR";
localparam C_DQ_IODRP2_SERDES_MODE_MASTER    = "MASTER";
localparam C_DQ_IODRP2_SERDES_MODE_SLAVE     = "SLAVE";

localparam C_DQS_IODRP2_DATA_RATE             = "SDR";
localparam C_DQS_IODRP2_SERDES_MODE_MASTER    = "MASTER";
localparam C_DQS_IODRP2_SERDES_MODE_SLAVE     = "SLAVE";



     


// MIG always set the below ADD_LATENCY to zero
localparam  C_MEM_DDR3_ADD_LATENCY      =  "OFF";
localparam  C_MEM_DDR2_ADD_LATENCY      =  0; 
localparam  C_MEM_MOBILE_TC_SR          =  0; // not supported

     
//////////////////////////////////////////////////////////////////////////////////
                                              // Attribute Declarations
                                              // Attributes set from GUI
                                              //
                                         //
   // the local param for the time slot varis according to User Port Configuration  
   // This section also needs to be customized when gernerating wrapper outputs.
   //*****************************************************************************


// For Configuration 1  and this section will be used in RAW file
localparam arbtimeslot0   = {C_ARB_TIME_SLOT_0   };
localparam arbtimeslot1   = {C_ARB_TIME_SLOT_1   };
localparam arbtimeslot2   = {C_ARB_TIME_SLOT_2   };
localparam arbtimeslot3   = {C_ARB_TIME_SLOT_3   };
localparam arbtimeslot4   = {C_ARB_TIME_SLOT_4   };
localparam arbtimeslot5   = {C_ARB_TIME_SLOT_5   };
localparam arbtimeslot6   = {C_ARB_TIME_SLOT_6   };
localparam arbtimeslot7   = {C_ARB_TIME_SLOT_7   };
localparam arbtimeslot8   = {C_ARB_TIME_SLOT_8   };
localparam arbtimeslot9   = {C_ARB_TIME_SLOT_9   };
localparam arbtimeslot10  = {C_ARB_TIME_SLOT_10  };
localparam arbtimeslot11  = {C_ARB_TIME_SLOT_11  };


// convert the memory timing to memory clock units. I
localparam MEM_RAS_VAL  = ((C_MEM_TRAS + C_MEMCLK_PERIOD -1) /C_MEMCLK_PERIOD);
localparam MEM_RCD_VAL  = ((C_MEM_TRCD  + C_MEMCLK_PERIOD -1) /C_MEMCLK_PERIOD);
localparam MEM_REFI_VAL = ((C_MEM_TREFI + C_MEMCLK_PERIOD -1) /C_MEMCLK_PERIOD) - 25;
localparam MEM_RFC_VAL  = ((C_MEM_TRFC  + C_MEMCLK_PERIOD -1) /C_MEMCLK_PERIOD);
localparam MEM_RP_VAL   = ((C_MEM_TRP   + C_MEMCLK_PERIOD -1) /C_MEMCLK_PERIOD);
localparam MEM_WR_VAL   = ((C_MEM_TWR   + C_MEMCLK_PERIOD -1) /C_MEMCLK_PERIOD);
localparam MEM_RTP_CK    = cdiv(C_MEM_TRTP,C_MEMCLK_PERIOD);
localparam MEM_RTP_VAL = (C_MEM_TYPE == "DDR3") ? (MEM_RTP_CK < 4) ? 4 : MEM_RTP_CK
                                               : (MEM_RTP_CK < 2) ? 2 : MEM_RTP_CK;
localparam MEM_WTR_VAL  = (C_MEM_TYPE == "DDR")   ? 2 :
                          (C_MEM_TYPE == "DDR3")  ? 4 : 
                          (C_MEM_TYPE == "MDDR")  ? C_MEM_TWTR : 
                          (C_MEM_TYPE == "LPDDR")  ? C_MEM_TWTR : 
                          ((C_MEM_TYPE == "DDR2") && (((C_MEM_TWTR  + C_MEMCLK_PERIOD -1) /C_MEMCLK_PERIOD) > 2)) ? ((C_MEM_TWTR  + C_MEMCLK_PERIOD -1) /C_MEMCLK_PERIOD) : 
                          (C_MEM_TYPE == "DDR2")  ? 2 
                                                  : 3 ;
localparam  C_MEM_DDR2_WRT_RECOVERY = (C_MEM_TYPE != "DDR2") ? 5: ((C_MEM_TWR   + C_MEMCLK_PERIOD -1) /C_MEMCLK_PERIOD);
localparam  C_MEM_DDR3_WRT_RECOVERY = (C_MEM_TYPE != "DDR3") ? 5: ((C_MEM_TWR   + C_MEMCLK_PERIOD -1) /C_MEMCLK_PERIOD);
//localparam MEM_TYPE = (C_MEM_TYPE == "LPDDR") ? "MDDR": C_MEM_TYPE;



////////////////////////////////////////////////////////////////////////////
// wire Declarations
////////////////////////////////////////////////////////////////////////////





wire [31:0]  addr_in0;
reg [127:0]  allzero = 0;


// UNISIM Model <-> IOI
//dqs clock network interface
wire       dqs_out_p;              
wire       dqs_out_n;              

wire       dqs_sys_p;              //from dqs_gen to IOclk network
wire       dqs_sys_n;              //from dqs_gen to IOclk network
wire       udqs_sys_p;
wire       udqs_sys_n;

wire       dqs_p;                  // open net now ?
wire       dqs_n;                  // open net now ?



// IOI and IOB enable/tristate interface
wire dqIO_w_en_0;                //enable DQ pads
wire dqsIO_w_en_90_p;            //enable p side of DQS
wire dqsIO_w_en_90_n;            //enable n side of DQS


//memory chip control interface
wire [14:0]   address_90;
wire [2:0]    ba_90;     
wire          ras_90;
wire          cas_90;
wire          we_90 ;
wire          cke_90;
wire          odt_90;
wire          rst_90;

// calibration IDELAY control  signals
wire          ioi_drp_clk;          //DRP interface - synchronous clock output
wire  [4:0]   ioi_drp_addr;         //DRP interface - IOI selection
wire          ioi_drp_sdo;          //DRP interface - serial output for commmands
wire          ioi_drp_sdi;          //DRP interface - serial input for commands
wire          ioi_drp_cs;           //DRP interface - chip select doubles as DONE signal
wire          ioi_drp_add;          //DRP interface - serial address signal
wire          ioi_drp_broadcast;  
wire          ioi_drp_train;    


   // Calibration datacapture siganls
   
wire  [3:0]dqdonecount; //select signal for the datacapture 16 to 1 mux
wire  dq_in_p;          //positive signal sent to calibration logic
wire  dq_in_n;          //negative signal sent to calibration logic
wire  cal_done;   
   

//DQS calibration interface
wire       udqs_n;
wire       udqs_p;


wire            udqs_dqocal_p;
wire            udqs_dqocal_n;


// MUI enable interface
wire df_en_n90  ;

//INTERNAL SIGNAL FOR DRP chain
// IOI <-> MUI
wire ioi_int_tmp;

wire [15:0]dqo_n;  
wire [15:0]dqo_p;  
wire dqnlm;      
wire dqplm;      
wire dqnum;      
wire dqpum;      


// IOI <-> IOB   routes
wire  [C_MEM_ADDR_WIDTH-1:0]ioi_addr; 
wire  [C_MEM_BANKADDR_WIDTH-1:0]ioi_ba;    
wire  ioi_cas;   
wire  ioi_ck;    
wire  ioi_ckn;    
wire  ioi_cke;   
wire  [C_NUM_DQ_PINS-1:0]ioi_dq; 
wire  ioi_dqs;   
wire  ioi_dqsn;
wire  ioi_udqs;
wire  ioi_udqsn;   
wire  ioi_odt;   
wire  ioi_ras;   
wire  ioi_rst;   
wire  ioi_we;   
wire  ioi_udm;
wire  ioi_ldm;

wire  [15:0] in_dq;
wire  [C_NUM_DQ_PINS-1:0] in_pre_dq;



wire            in_dqs;     
wire            in_pre_dqsp;
wire            in_pre_dqsn;
wire            in_pre_udqsp;
wire            in_pre_udqsn;
wire            in_udqs;
     // Memory tri-state control signals
wire  [C_MEM_ADDR_WIDTH-1:0]t_addr; 
wire  [C_MEM_BANKADDR_WIDTH-1:0]t_ba;    
wire  t_cas;
wire  t_ck ;
wire  t_ckn;
wire  t_cke;
wire  [C_NUM_DQ_PINS-1:0]t_dq;
wire  t_dqs;     
wire  t_dqsn;
wire  t_udqs;
wire  t_udqsn;
wire  t_odt;     
wire  t_ras;     
wire  t_rst;     
wire  t_we ;     


wire  t_udm  ;
wire  t_ldm  ;



wire             idelay_dqs_ioi_s;
wire             idelay_dqs_ioi_m;
wire             idelay_udqs_ioi_s;
wire             idelay_udqs_ioi_m;


wire  dqs_pin;
wire  udqs_pin;

// USER Interface signals


// translated memory addresses
wire [14:0]p0_cmd_ra;
wire [2:0]p0_cmd_ba; 
wire [11:0]p0_cmd_ca;
wire [14:0]p1_cmd_ra;
wire [2:0]p1_cmd_ba; 
wire [11:0]p1_cmd_ca;
wire [14:0]p2_cmd_ra;
wire [2:0]p2_cmd_ba; 
wire [11:0]p2_cmd_ca;
wire [14:0]p3_cmd_ra;
wire [2:0]p3_cmd_ba; 
wire [11:0]p3_cmd_ca;
wire [14:0]p4_cmd_ra;
wire [2:0]p4_cmd_ba; 
wire [11:0]p4_cmd_ca;
wire [14:0]p5_cmd_ra;
wire [2:0]p5_cmd_ba; 
wire [11:0]p5_cmd_ca;

   // user command wires mapped from logical ports to physical ports
wire        mig_p0_arb_en;   
wire        mig_p0_cmd_clk;    
wire        mig_p0_cmd_en;     
wire [14:0] mig_p0_cmd_ra;     
wire [2:0]  mig_p0_cmd_ba;     
wire [11:0] mig_p0_cmd_ca;     

wire [2:0]  mig_p0_cmd_instr;   
wire [5:0]  mig_p0_cmd_bl;      
wire        mig_p0_cmd_empty;   
wire        mig_p0_cmd_full;    


wire        mig_p1_arb_en;   
wire        mig_p1_cmd_clk;    
wire        mig_p1_cmd_en;     
wire [14:0] mig_p1_cmd_ra;     
wire [2:0] mig_p1_cmd_ba;     
wire [11:0] mig_p1_cmd_ca;     

wire [2:0]  mig_p1_cmd_instr;   
wire [5:0]  mig_p1_cmd_bl;      
wire        mig_p1_cmd_empty;   
wire        mig_p1_cmd_full;    

wire        mig_p2_arb_en;   
wire        mig_p2_cmd_clk;    
wire        mig_p2_cmd_en;     
wire [14:0] mig_p2_cmd_ra;     
wire [2:0] mig_p2_cmd_ba;     
wire [11:0] mig_p2_cmd_ca;     
                  
wire [2:0]  mig_p2_cmd_instr;   
wire [5:0]  mig_p2_cmd_bl;      
wire        mig_p2_cmd_empty;   
wire        mig_p2_cmd_full;    

wire        mig_p3_arb_en;   
wire        mig_p3_cmd_clk;    
wire        mig_p3_cmd_en;     
wire [14:0] mig_p3_cmd_ra;     
wire [2:0] mig_p3_cmd_ba;     
wire [11:0] mig_p3_cmd_ca;     

wire [2:0]  mig_p3_cmd_instr;   
wire [5:0]  mig_p3_cmd_bl;      
wire        mig_p3_cmd_empty;   
wire        mig_p3_cmd_full;    

wire        mig_p4_arb_en;   
wire        mig_p4_cmd_clk;    
wire        mig_p4_cmd_en;     
wire [14:0] mig_p4_cmd_ra;     
wire [2:0] mig_p4_cmd_ba;     
wire [11:0] mig_p4_cmd_ca;     

wire [2:0]  mig_p4_cmd_instr;   
wire [5:0]  mig_p4_cmd_bl;      
wire        mig_p4_cmd_empty;   
wire        mig_p4_cmd_full;    

wire        mig_p5_arb_en;   
wire        mig_p5_cmd_clk;    
wire        mig_p5_cmd_en;     
wire [14:0] mig_p5_cmd_ra;     
wire [2:0] mig_p5_cmd_ba;     
wire [11:0] mig_p5_cmd_ca;     

wire [2:0]  mig_p5_cmd_instr;   
wire [5:0]  mig_p5_cmd_bl;      
wire        mig_p5_cmd_empty;   
wire        mig_p5_cmd_full;    

wire        mig_p0_wr_clk;
wire        mig_p0_rd_clk;
wire        mig_p1_wr_clk;
wire        mig_p1_rd_clk;
wire        mig_p2_clk;
wire        mig_p3_clk;
wire        mig_p4_clk;
wire        mig_p5_clk;

wire       mig_p0_wr_en;
wire       mig_p0_rd_en;
wire       mig_p1_wr_en;
wire       mig_p1_rd_en;
wire       mig_p2_en;
wire       mig_p3_en; 
wire       mig_p4_en; 
wire       mig_p5_en; 


wire [31:0]mig_p0_wr_data;
wire [31:0]mig_p1_wr_data;
wire [31:0]mig_p2_wr_data;
wire [31:0]mig_p3_wr_data;
wire [31:0]mig_p4_wr_data;
wire [31:0]mig_p5_wr_data;


wire  [C_P0_MASK_SIZE-1:0]mig_p0_wr_mask;
wire  [C_P1_MASK_SIZE-1:0]mig_p1_wr_mask;
wire  [3:0]mig_p2_wr_mask;
wire  [3:0]mig_p3_wr_mask;
wire  [3:0]mig_p4_wr_mask;
wire  [3:0]mig_p5_wr_mask;


wire  [31:0]mig_p0_rd_data; 
wire  [31:0]mig_p1_rd_data; 
wire  [31:0]mig_p2_rd_data; 
wire  [31:0]mig_p3_rd_data; 
wire  [31:0]mig_p4_rd_data; 
wire  [31:0]mig_p5_rd_data; 

wire  mig_p0_rd_overflow;
wire  mig_p1_rd_overflow;
wire  mig_p2_overflow;
wire  mig_p3_overflow;

wire  mig_p4_overflow;
wire  mig_p5_overflow;

wire  mig_p0_wr_underrun;
wire  mig_p1_wr_underrun;
wire  mig_p2_underrun;  
wire  mig_p3_underrun;  
wire  mig_p4_underrun;  
wire  mig_p5_underrun;  

wire       mig_p0_rd_error;
wire       mig_p0_wr_error;
wire       mig_p1_rd_error;
wire       mig_p1_wr_error;
wire       mig_p2_error;    
wire       mig_p3_error;    
wire       mig_p4_error;    
wire       mig_p5_error;    


wire  [6:0]mig_p0_wr_count;
wire  [6:0]mig_p1_wr_count;
wire  [6:0]mig_p0_rd_count;
wire  [6:0]mig_p1_rd_count;

wire  [6:0]mig_p2_count;
wire  [6:0]mig_p3_count;
wire  [6:0]mig_p4_count;
wire  [6:0]mig_p5_count;

wire  mig_p0_wr_full;
wire  mig_p1_wr_full;

wire mig_p0_rd_empty;
wire mig_p1_rd_empty;
wire mig_p0_wr_empty;
wire mig_p1_wr_empty;
wire mig_p0_rd_full;
wire mig_p1_rd_full;
wire mig_p2_full;
wire mig_p3_full;
wire mig_p4_full;
wire mig_p5_full;
wire mig_p2_empty;
wire mig_p3_empty;
wire mig_p4_empty;
wire mig_p5_empty;

// SELFREESH control signal for suspend feature
wire selfrefresh_mcb_enter;
wire selfrefresh_mcb_mode ;
// Testing Interface signals
wire           tst_cmd_test_en;
wire   [7:0]   tst_sel;
wire   [15:0]  tst_in;
wire           tst_scan_clk;
wire           tst_scan_rst;
wire           tst_scan_set;
wire           tst_scan_en;
wire           tst_scan_in;
wire           tst_scan_mode;

wire           p0w_tst_en;
wire           p0r_tst_en;
wire           p1w_tst_en;
wire           p1r_tst_en;
wire           p2_tst_en;
wire           p3_tst_en;
wire           p4_tst_en;
wire           p5_tst_en;

wire           p0_tst_wr_clk_en;
wire           p0_tst_rd_clk_en;
wire           p1_tst_wr_clk_en;
wire           p1_tst_rd_clk_en;
wire           p2_tst_clk_en;
wire           p3_tst_clk_en;
wire           p4_tst_clk_en;
wire           p5_tst_clk_en;

wire   [3:0]   p0w_tst_wr_mode;
wire   [3:0]   p0r_tst_mode;
wire   [3:0]   p1w_tst_wr_mode;
wire   [3:0]   p1r_tst_mode;
wire   [3:0]   p2_tst_mode;
wire   [3:0]   p3_tst_mode;
wire   [3:0]   p4_tst_mode;
wire   [3:0]   p5_tst_mode;

wire           p0r_tst_pin_en;
wire           p0w_tst_pin_en;
wire           p1r_tst_pin_en;
wire           p1w_tst_pin_en;
wire           p2_tst_pin_en;
wire           p3_tst_pin_en;
wire           p4_tst_pin_en;
wire           p5_tst_pin_en;
wire           p0w_tst_overflow;
wire           p1w_tst_overflow;

wire  [3:0]   p0r_tst_mask_o;
wire  [3:0]   p0w_tst_mask_o;
wire  [3:0]   p1r_tst_mask_o;
wire  [3:0]   p1w_tst_mask_o;
wire  [3:0]   p2_tst_mask_o;
wire  [3:0]   p3_tst_mask_o;
wire  [3:0]   p4_tst_mask_o;
wire  [3:0]   p5_tst_mask_o;
wire  [3:0]   p0r_tst_wr_mask;

wire  [3:0]   p1r_tst_wr_mask;
wire [31:0]  p1r_tst_wr_data;
wire [31:0]  p0r_tst_wr_data;
wire [31:0]   p0w_tst_rd_data;
wire [31:0]   p1w_tst_rd_data;

wire  [38:0]  tst_cmd_out;
wire           MCB_SYSRST;
wire ioclk0;
wire ioclk90;
wire mcb_ui_clk;                               
wire hard_done_cal;                                
wire cke_train;
//testing
wire       ioi_drp_update;
wire [7:0] aux_sdi_sdo;

wire [4:0] mcb_ui_addr;
wire [3:0] mcb_ui_dqcount;
reg  syn_uiclk_pll_lock;
reg syn1_sys_rst, syn2_sys_rst;

wire int_sys_rst /* synthesis syn_maxfan = 1 */;
// synthesis attribute max_fanout of int_sys_rst is 1

reg selfrefresh_enter_r1,selfrefresh_enter_r2,selfrefresh_enter_r3;
reg gated_pll_lock;	   
reg soft_cal_selfrefresh_req;
reg [15:0]    wait_200us_counter;
reg           cke_train_reg;        
reg           wait_200us_done_r1,wait_200us_done_r2;
reg normal_operation_window;

assign ioclk0 = sysclk_2x;
assign ioclk90 = sysclk_2x_180;



// logic to determine if Memory  is SELFREFRESH mode operation or NORMAL  mode.
always @ (posedge ui_clk)
begin 
if (sys_rst)   
   normal_operation_window <= 1'b1;
else if (selfrefresh_enter_r2 || selfrefresh_mode)
   normal_operation_window <= 1'b0;
else if (~selfrefresh_enter_r2 && ~selfrefresh_mode)
   normal_operation_window <= 1'b1;
else
   normal_operation_window <= normal_operation_window;

end   


always @ (*)
begin
if (normal_operation_window)
   gated_pll_lock = pll_lock;
else
   gated_pll_lock = syn_uiclk_pll_lock;
end


//assign int_sys_rst =  sys_rst | ~gated_pll_lock;
always @ (posedge ui_clk)
begin 
  if (~selfrefresh_enter && ~selfrefresh_mode)
   syn_uiclk_pll_lock <= pll_lock;
   
end   

// int_sys_rst will be asserted if pll lose lock during normal operation.
// It uses the syn_uiclk_pll_lock version when it is entering suspend window , hence
// reset will not be generated.   
assign int_sys_rst =  sys_rst | ~gated_pll_lock;



// synchronize the selfrefresh_enter 
always @ (posedge ui_clk)
if (sys_rst)
   begin
      selfrefresh_enter_r1 <= 1'b0;
      selfrefresh_enter_r2 <= 1'b0;
      selfrefresh_enter_r3 <= 1'b0;
   end
else
   begin
      selfrefresh_enter_r1 <= selfrefresh_enter;
      selfrefresh_enter_r2 <= selfrefresh_enter_r1;
      selfrefresh_enter_r3 <= selfrefresh_enter_r2;
   end



// The soft_cal_selfrefresh siganl is conditioned before connect to mcb_soft_calibration module.
// It will not deassert selfrefresh_mcb_enter to MCB until input pll_lock reestablished in system.
// This is to ensure the IOI stables before issued a selfrefresh exit command to dram.
always @ (posedge ui_clk)
begin 
  if (sys_rst)
   soft_cal_selfrefresh_req <= 1'b0;
  else if (selfrefresh_enter_r3)
     soft_cal_selfrefresh_req <= 1'b1;
  else if (~selfrefresh_enter_r3 && pll_lock)
     soft_cal_selfrefresh_req <= 1'b0;
  else
     soft_cal_selfrefresh_req <= soft_cal_selfrefresh_req;
  
end   


//Address Remapping
// Byte Address remapping
// 
// Bank Address[x:0] & Row Address[x:0]  & Column Address[x:0]
// column address remap for port 0
 generate //  port bus remapping sections for CONFIG 2   15,3,12

if(C_NUM_DQ_PINS == 16) begin : x16_Addr
           if (C_MEM_ADDR_ORDER == "ROW_BANK_COLUMN") begin  // C_MEM_ADDR_ORDER = 0 : Bank Row  Column
                 // port 0 address remapping
                
                
                if (C_MEM_ADDR_WIDTH == 15)   //Row        
                       assign p0_cmd_ra = p0_cmd_byte_addr[C_MEM_ADDR_WIDTH + C_MEM_BANKADDR_WIDTH + C_MEM_NUM_COL_BITS   : C_MEM_BANKADDR_WIDTH + C_MEM_NUM_COL_BITS + 1];                         
                else
                       assign p0_cmd_ra = {allzero[14 : C_MEM_ADDR_WIDTH] ,  p0_cmd_byte_addr[C_MEM_ADDR_WIDTH + C_MEM_BANKADDR_WIDTH + C_MEM_NUM_COL_BITS   :C_MEM_BANKADDR_WIDTH +  C_MEM_NUM_COL_BITS + 1]};                         


                if (C_MEM_BANKADDR_WIDTH  == 3 )  //Bank
                       assign p0_cmd_ba = p0_cmd_byte_addr[C_MEM_BANKADDR_WIDTH +  C_MEM_NUM_COL_BITS   :  C_MEM_NUM_COL_BITS + 1];
                else
                       assign p0_cmd_ba = {allzero[2 : C_MEM_BANKADDR_WIDTH] , p0_cmd_byte_addr[C_MEM_BANKADDR_WIDTH +   C_MEM_NUM_COL_BITS   :  C_MEM_NUM_COL_BITS + 1]};
                
                if (C_MEM_NUM_COL_BITS == 12)  //Column
                       assign p0_cmd_ca = p0_cmd_byte_addr[C_MEM_NUM_COL_BITS : 1];
                else
                       assign p0_cmd_ca = {allzero[12:C_MEM_NUM_COL_BITS + 1], p0_cmd_byte_addr[C_MEM_NUM_COL_BITS : 1]};

                
                 // port 1 address remapping
                
                
                if (C_MEM_ADDR_WIDTH == 15)   //Row        
                       assign p1_cmd_ra = p1_cmd_byte_addr[C_MEM_ADDR_WIDTH + C_MEM_BANKADDR_WIDTH + C_MEM_NUM_COL_BITS   : C_MEM_BANKADDR_WIDTH + C_MEM_NUM_COL_BITS + 1];                         
                else
                       assign p1_cmd_ra = {allzero[14 : C_MEM_ADDR_WIDTH] ,  p1_cmd_byte_addr[C_MEM_ADDR_WIDTH + C_MEM_BANKADDR_WIDTH + C_MEM_NUM_COL_BITS   :C_MEM_BANKADDR_WIDTH +  C_MEM_NUM_COL_BITS + 1]};                         


                if (C_MEM_BANKADDR_WIDTH  == 3 )  //Bank
                       assign p1_cmd_ba = p1_cmd_byte_addr[C_MEM_BANKADDR_WIDTH +  C_MEM_NUM_COL_BITS   :  C_MEM_NUM_COL_BITS + 1];
                else
                       assign p1_cmd_ba = {allzero[2 : C_MEM_BANKADDR_WIDTH] , p1_cmd_byte_addr[C_MEM_BANKADDR_WIDTH +   C_MEM_NUM_COL_BITS   :  C_MEM_NUM_COL_BITS + 1]};
                
                if (C_MEM_NUM_COL_BITS == 12)  //Column
                       assign p1_cmd_ca = p1_cmd_byte_addr[C_MEM_NUM_COL_BITS : 1];
                else
                       assign p1_cmd_ca = {allzero[12:C_MEM_NUM_COL_BITS  + 1], p1_cmd_byte_addr[C_MEM_NUM_COL_BITS : 1]};

                 // port 2 address remapping
                
                
                if (C_MEM_ADDR_WIDTH == 15)   //Row        
                       assign p2_cmd_ra = p2_cmd_byte_addr[C_MEM_ADDR_WIDTH + C_MEM_BANKADDR_WIDTH + C_MEM_NUM_COL_BITS   : C_MEM_BANKADDR_WIDTH + C_MEM_NUM_COL_BITS + 1];                         
                else
                       assign p2_cmd_ra = {allzero[14 : C_MEM_ADDR_WIDTH] ,  p2_cmd_byte_addr[C_MEM_ADDR_WIDTH + C_MEM_BANKADDR_WIDTH + C_MEM_NUM_COL_BITS   :C_MEM_BANKADDR_WIDTH +  C_MEM_NUM_COL_BITS + 1]};                         


                if (C_MEM_BANKADDR_WIDTH  == 3 )  //Bank
                       assign p2_cmd_ba = p2_cmd_byte_addr[C_MEM_BANKADDR_WIDTH +  C_MEM_NUM_COL_BITS   :  C_MEM_NUM_COL_BITS + 1];
                else
                       assign p2_cmd_ba = {allzero[2 : C_MEM_BANKADDR_WIDTH] , p2_cmd_byte_addr[C_MEM_BANKADDR_WIDTH +   C_MEM_NUM_COL_BITS   :  C_MEM_NUM_COL_BITS + 1]};
                
                if (C_MEM_NUM_COL_BITS == 12)  //Column
                       assign p2_cmd_ca = p2_cmd_byte_addr[C_MEM_NUM_COL_BITS : 1];
                else
                       assign p2_cmd_ca = {allzero[12:C_MEM_NUM_COL_BITS + 1], p2_cmd_byte_addr[C_MEM_NUM_COL_BITS : 1]};

                 // port 3 address remapping
                
                
                if (C_MEM_ADDR_WIDTH == 15)   //Row        
                       assign p3_cmd_ra = p3_cmd_byte_addr[C_MEM_ADDR_WIDTH + C_MEM_BANKADDR_WIDTH + C_MEM_NUM_COL_BITS   : C_MEM_BANKADDR_WIDTH + C_MEM_NUM_COL_BITS + 1];                         
                else
                       assign p3_cmd_ra = {allzero[14 : C_MEM_ADDR_WIDTH] ,  p3_cmd_byte_addr[C_MEM_ADDR_WIDTH + C_MEM_BANKADDR_WIDTH + C_MEM_NUM_COL_BITS   :C_MEM_BANKADDR_WIDTH +  C_MEM_NUM_COL_BITS + 1]};                         


                if (C_MEM_BANKADDR_WIDTH  == 3 )  //Bank
                       assign p3_cmd_ba = p3_cmd_byte_addr[C_MEM_BANKADDR_WIDTH +  C_MEM_NUM_COL_BITS   :  C_MEM_NUM_COL_BITS + 1];
                else
                       assign p3_cmd_ba = {allzero[2 : C_MEM_BANKADDR_WIDTH] , p3_cmd_byte_addr[C_MEM_BANKADDR_WIDTH +   C_MEM_NUM_COL_BITS   :  C_MEM_NUM_COL_BITS + 1]};
                
                if (C_MEM_NUM_COL_BITS == 12)  //Column
                       assign p3_cmd_ca = p3_cmd_byte_addr[C_MEM_NUM_COL_BITS : 1];
                else
                       assign p3_cmd_ca = {allzero[12:C_MEM_NUM_COL_BITS + 1], p3_cmd_byte_addr[C_MEM_NUM_COL_BITS : 1]};

                 // port 4 address remapping
                
                
                if (C_MEM_ADDR_WIDTH == 15)   //Row        
                       assign p4_cmd_ra = p4_cmd_byte_addr[C_MEM_ADDR_WIDTH + C_MEM_BANKADDR_WIDTH + C_MEM_NUM_COL_BITS   : C_MEM_BANKADDR_WIDTH + C_MEM_NUM_COL_BITS + 1];                         
                else
                       assign p4_cmd_ra = {allzero[14 : C_MEM_ADDR_WIDTH] ,  p4_cmd_byte_addr[C_MEM_ADDR_WIDTH + C_MEM_BANKADDR_WIDTH + C_MEM_NUM_COL_BITS   :C_MEM_BANKADDR_WIDTH +  C_MEM_NUM_COL_BITS + 1]};                         


                if (C_MEM_BANKADDR_WIDTH  == 3 )  //Bank
                       assign p4_cmd_ba = p4_cmd_byte_addr[C_MEM_BANKADDR_WIDTH +  C_MEM_NUM_COL_BITS   :  C_MEM_NUM_COL_BITS + 1];
                else
                       assign p4_cmd_ba = {allzero[2 : C_MEM_BANKADDR_WIDTH] , p4_cmd_byte_addr[C_MEM_BANKADDR_WIDTH +   C_MEM_NUM_COL_BITS   :  C_MEM_NUM_COL_BITS + 1]};
                
                if (C_MEM_NUM_COL_BITS == 12)  //Column
                       assign p4_cmd_ca = p4_cmd_byte_addr[C_MEM_NUM_COL_BITS : 1];
                else
                       assign p4_cmd_ca = {allzero[12:C_MEM_NUM_COL_BITS + 1], p4_cmd_byte_addr[C_MEM_NUM_COL_BITS : 1]};

                 // port 5 address remapping
                
                
                if (C_MEM_ADDR_WIDTH == 15)   //Row        
                       assign p5_cmd_ra = p5_cmd_byte_addr[C_MEM_ADDR_WIDTH + C_MEM_BANKADDR_WIDTH + C_MEM_NUM_COL_BITS   : C_MEM_BANKADDR_WIDTH + C_MEM_NUM_COL_BITS + 1];                         
                else
                       assign p5_cmd_ra = {allzero[14 : C_MEM_ADDR_WIDTH] ,  p5_cmd_byte_addr[C_MEM_ADDR_WIDTH + C_MEM_BANKADDR_WIDTH + C_MEM_NUM_COL_BITS   :C_MEM_BANKADDR_WIDTH +  C_MEM_NUM_COL_BITS + 1]};                         


                if (C_MEM_BANKADDR_WIDTH  == 3 )  //Bank
                       assign p5_cmd_ba = p5_cmd_byte_addr[C_MEM_BANKADDR_WIDTH +  C_MEM_NUM_COL_BITS   :  C_MEM_NUM_COL_BITS + 1];
                else
                       assign p5_cmd_ba = {allzero[2 : C_MEM_BANKADDR_WIDTH] , p5_cmd_byte_addr[C_MEM_BANKADDR_WIDTH +   C_MEM_NUM_COL_BITS   :  C_MEM_NUM_COL_BITS + 1]};
                
                if (C_MEM_NUM_COL_BITS == 12)  //Column
                       assign p5_cmd_ca = p5_cmd_byte_addr[C_MEM_NUM_COL_BITS : 1];
                else
                       assign p5_cmd_ca = {allzero[12:C_MEM_NUM_COL_BITS + 1], p5_cmd_byte_addr[C_MEM_NUM_COL_BITS : 1]};


                
                
                end
                
          else  // ***************C_MEM_ADDR_ORDER = 1 :  Row Bank Column
              begin
                 // port 0 address remapping

                if (C_MEM_BANKADDR_WIDTH  == 3 )  //Bank
                       assign p0_cmd_ba = p0_cmd_byte_addr[C_MEM_BANKADDR_WIDTH + C_MEM_ADDR_WIDTH +  C_MEM_NUM_COL_BITS  : C_MEM_ADDR_WIDTH + C_MEM_NUM_COL_BITS + 1];
                else
                       assign p0_cmd_ba = {allzero[2 : C_MEM_BANKADDR_WIDTH] , p0_cmd_byte_addr[C_MEM_BANKADDR_WIDTH + C_MEM_ADDR_WIDTH +  C_MEM_NUM_COL_BITS  : C_MEM_ADDR_WIDTH + C_MEM_NUM_COL_BITS + 1]};
                
                
                if (C_MEM_ADDR_WIDTH == 15)           
                       assign p0_cmd_ra = p0_cmd_byte_addr[C_MEM_ADDR_WIDTH + C_MEM_NUM_COL_BITS   : C_MEM_NUM_COL_BITS + 1];                         
                else
                       assign p0_cmd_ra = {allzero[14 : C_MEM_ADDR_WIDTH] ,  p0_cmd_byte_addr[C_MEM_ADDR_WIDTH + C_MEM_NUM_COL_BITS   : C_MEM_NUM_COL_BITS + 1]};                         
                
                if (C_MEM_NUM_COL_BITS == 12)  //Column
                       assign p0_cmd_ca = p0_cmd_byte_addr[C_MEM_NUM_COL_BITS : 1];
                else
                       assign p0_cmd_ca = {allzero[12:C_MEM_NUM_COL_BITS + 1], p0_cmd_byte_addr[C_MEM_NUM_COL_BITS : 1]};


                 // port 1 address remapping

                if (C_MEM_BANKADDR_WIDTH  == 3 )  //Bank
                       assign p1_cmd_ba = p1_cmd_byte_addr[C_MEM_BANKADDR_WIDTH + C_MEM_ADDR_WIDTH +  C_MEM_NUM_COL_BITS  : C_MEM_ADDR_WIDTH + C_MEM_NUM_COL_BITS + 1];
                else
                       assign p1_cmd_ba = {allzero[2 : C_MEM_BANKADDR_WIDTH] , p1_cmd_byte_addr[C_MEM_BANKADDR_WIDTH + C_MEM_ADDR_WIDTH +  C_MEM_NUM_COL_BITS  : C_MEM_ADDR_WIDTH + C_MEM_NUM_COL_BITS + 1]};
                
                
                if (C_MEM_ADDR_WIDTH == 15)           
                       assign p1_cmd_ra = p1_cmd_byte_addr[C_MEM_ADDR_WIDTH + C_MEM_NUM_COL_BITS   : C_MEM_NUM_COL_BITS + 1];                         
                else
                       assign p1_cmd_ra = {allzero[14 : C_MEM_ADDR_WIDTH] ,  p1_cmd_byte_addr[C_MEM_ADDR_WIDTH + C_MEM_NUM_COL_BITS   : C_MEM_NUM_COL_BITS + 1]};                         
                
                if (C_MEM_NUM_COL_BITS == 12)  //Column
                       assign p1_cmd_ca = p1_cmd_byte_addr[C_MEM_NUM_COL_BITS : 1];
                else
                       assign p1_cmd_ca = {allzero[12:C_MEM_NUM_COL_BITS + 1], p1_cmd_byte_addr[C_MEM_NUM_COL_BITS : 1]};

                 // port 2 address remapping

                if (C_MEM_BANKADDR_WIDTH  == 3 )  //Bank
                       assign p2_cmd_ba = p2_cmd_byte_addr[C_MEM_BANKADDR_WIDTH + C_MEM_ADDR_WIDTH +  C_MEM_NUM_COL_BITS  : C_MEM_ADDR_WIDTH + C_MEM_NUM_COL_BITS + 1];
                else
                       assign p2_cmd_ba = {allzero[2 : C_MEM_BANKADDR_WIDTH] , p2_cmd_byte_addr[C_MEM_BANKADDR_WIDTH + C_MEM_ADDR_WIDTH +  C_MEM_NUM_COL_BITS  : C_MEM_ADDR_WIDTH + C_MEM_NUM_COL_BITS + 1]};
                
                
                if (C_MEM_ADDR_WIDTH == 15)           
                       assign p2_cmd_ra = p2_cmd_byte_addr[C_MEM_ADDR_WIDTH + C_MEM_NUM_COL_BITS   : C_MEM_NUM_COL_BITS + 1];                         
                else
                       assign p2_cmd_ra = {allzero[14 : C_MEM_ADDR_WIDTH] ,  p2_cmd_byte_addr[C_MEM_ADDR_WIDTH + C_MEM_NUM_COL_BITS   : C_MEM_NUM_COL_BITS + 1]};                         
                
                if (C_MEM_NUM_COL_BITS == 12)  //Column
                       assign p2_cmd_ca = p2_cmd_byte_addr[C_MEM_NUM_COL_BITS : 1];
                else
                       assign p2_cmd_ca = {allzero[12:C_MEM_NUM_COL_BITS + 1], p2_cmd_byte_addr[C_MEM_NUM_COL_BITS : 1]};

                 // port 3 address remapping

                if (C_MEM_BANKADDR_WIDTH  == 3 )  //Bank
                       assign p3_cmd_ba = p3_cmd_byte_addr[C_MEM_BANKADDR_WIDTH + C_MEM_ADDR_WIDTH +  C_MEM_NUM_COL_BITS  : C_MEM_ADDR_WIDTH + C_MEM_NUM_COL_BITS + 1];
                else
                       assign p3_cmd_ba = {allzero[2 : C_MEM_BANKADDR_WIDTH] , p3_cmd_byte_addr[C_MEM_BANKADDR_WIDTH + C_MEM_ADDR_WIDTH +  C_MEM_NUM_COL_BITS  : C_MEM_ADDR_WIDTH + C_MEM_NUM_COL_BITS + 1]};
                
                
                if (C_MEM_ADDR_WIDTH == 15)           
                       assign p3_cmd_ra = p3_cmd_byte_addr[C_MEM_ADDR_WIDTH + C_MEM_NUM_COL_BITS   : C_MEM_NUM_COL_BITS + 1];                         
                else
                       assign p3_cmd_ra = {allzero[14 : C_MEM_ADDR_WIDTH] ,  p3_cmd_byte_addr[C_MEM_ADDR_WIDTH + C_MEM_NUM_COL_BITS   : C_MEM_NUM_COL_BITS + 1]};                         
                
                if (C_MEM_NUM_COL_BITS == 12)  //Column
                       assign p3_cmd_ca = p3_cmd_byte_addr[C_MEM_NUM_COL_BITS : 1];
                else
                       assign p3_cmd_ca = {allzero[12:C_MEM_NUM_COL_BITS + 1], p3_cmd_byte_addr[C_MEM_NUM_COL_BITS : 1]};

                 // port 4 address remapping

                if (C_MEM_BANKADDR_WIDTH  == 3 )  //Bank
                       assign p4_cmd_ba = p4_cmd_byte_addr[C_MEM_BANKADDR_WIDTH + C_MEM_ADDR_WIDTH +  C_MEM_NUM_COL_BITS  : C_MEM_ADDR_WIDTH + C_MEM_NUM_COL_BITS + 1];
                else
                       assign p4_cmd_ba = {allzero[2 : C_MEM_BANKADDR_WIDTH] , p4_cmd_byte_addr[C_MEM_BANKADDR_WIDTH + C_MEM_ADDR_WIDTH +  C_MEM_NUM_COL_BITS  : C_MEM_ADDR_WIDTH + C_MEM_NUM_COL_BITS + 1]};
                
                
                if (C_MEM_ADDR_WIDTH == 15)           
                       assign p4_cmd_ra = p4_cmd_byte_addr[C_MEM_ADDR_WIDTH + C_MEM_NUM_COL_BITS   : C_MEM_NUM_COL_BITS + 1];                         
                else
                       assign p4_cmd_ra = {allzero[14 : C_MEM_ADDR_WIDTH] ,  p4_cmd_byte_addr[C_MEM_ADDR_WIDTH + C_MEM_NUM_COL_BITS   : C_MEM_NUM_COL_BITS + 1]};                         
                
                if (C_MEM_NUM_COL_BITS == 12)  //Column
                       assign p4_cmd_ca = p4_cmd_byte_addr[C_MEM_NUM_COL_BITS : 1];
                else
                       assign p4_cmd_ca = {allzero[12:C_MEM_NUM_COL_BITS + 1], p4_cmd_byte_addr[C_MEM_NUM_COL_BITS : 1]};

                 // port 5 address remapping

                if (C_MEM_BANKADDR_WIDTH  == 3 )  //Bank
                       assign p5_cmd_ba = p5_cmd_byte_addr[C_MEM_BANKADDR_WIDTH + C_MEM_ADDR_WIDTH +  C_MEM_NUM_COL_BITS  : C_MEM_ADDR_WIDTH + C_MEM_NUM_COL_BITS + 1];
                else
                       assign p5_cmd_ba = {allzero[2 : C_MEM_BANKADDR_WIDTH] , p5_cmd_byte_addr[C_MEM_BANKADDR_WIDTH + C_MEM_ADDR_WIDTH +  C_MEM_NUM_COL_BITS  : C_MEM_ADDR_WIDTH + C_MEM_NUM_COL_BITS + 1]};
                
                
                if (C_MEM_ADDR_WIDTH == 15)           
                       assign p5_cmd_ra = p5_cmd_byte_addr[C_MEM_ADDR_WIDTH + C_MEM_NUM_COL_BITS   : C_MEM_NUM_COL_BITS + 1];                         
                else
                       assign p5_cmd_ra = {allzero[14 : C_MEM_ADDR_WIDTH] ,  p5_cmd_byte_addr[C_MEM_ADDR_WIDTH + C_MEM_NUM_COL_BITS   : C_MEM_NUM_COL_BITS + 1]};                         
                
                if (C_MEM_NUM_COL_BITS == 12)  //Column
                       assign p5_cmd_ca = p5_cmd_byte_addr[C_MEM_NUM_COL_BITS : 1];
                else
                       assign p5_cmd_ca = {allzero[12:C_MEM_NUM_COL_BITS + 1], p5_cmd_byte_addr[C_MEM_NUM_COL_BITS : 1]};

         
              end
       
end else if(C_NUM_DQ_PINS == 8) begin : x8_Addr
           if (C_MEM_ADDR_ORDER == "ROW_BANK_COLUMN") begin  // C_MEM_ADDR_ORDER = 1 : Bank Row Column
                 // port 0 address remapping

                 if (C_MEM_ADDR_WIDTH == 15)  //Row
                          assign p0_cmd_ra = p0_cmd_byte_addr[C_MEM_ADDR_WIDTH + C_MEM_BANKADDR_WIDTH +  C_MEM_NUM_COL_BITS - 1  : C_MEM_BANKADDR_WIDTH +  C_MEM_NUM_COL_BITS ];
                 else
                          assign p0_cmd_ra = {allzero[14 : C_MEM_ADDR_WIDTH] , p0_cmd_byte_addr[C_MEM_ADDR_WIDTH + C_MEM_BANKADDR_WIDTH +  C_MEM_NUM_COL_BITS - 1  : C_MEM_BANKADDR_WIDTH +  C_MEM_NUM_COL_BITS ]};


                 if (C_MEM_BANKADDR_WIDTH  == 3 )  //Bank
                          assign p0_cmd_ba = p0_cmd_byte_addr[C_MEM_BANKADDR_WIDTH +  C_MEM_NUM_COL_BITS - 1 :  C_MEM_NUM_COL_BITS ];  //14,3,10
                 else
                          assign p0_cmd_ba = {allzero[2 : C_MEM_BANKADDR_WIDTH],  
                                   p0_cmd_byte_addr[C_MEM_BANKADDR_WIDTH +  C_MEM_NUM_COL_BITS - 1 : C_MEM_NUM_COL_BITS ]};  //14,3,10
                 
                 
                 if (C_MEM_NUM_COL_BITS == 12)  //Column
                          assign p0_cmd_ca[11:0] = p0_cmd_byte_addr[C_MEM_NUM_COL_BITS - 1 : 0];
                 else
                          assign p0_cmd_ca[11:0] = {allzero[11 : C_MEM_NUM_COL_BITS] , p0_cmd_byte_addr[C_MEM_NUM_COL_BITS - 1 : 0]};
                 
                 
                // port 1 address remapping
                 if (C_MEM_ADDR_WIDTH == 15)  //Row
                          assign p1_cmd_ra = p1_cmd_byte_addr[C_MEM_ADDR_WIDTH + C_MEM_BANKADDR_WIDTH +  C_MEM_NUM_COL_BITS - 1  : C_MEM_BANKADDR_WIDTH +  C_MEM_NUM_COL_BITS ];
                 else
                          assign p1_cmd_ra = {allzero[14 : C_MEM_ADDR_WIDTH] , p1_cmd_byte_addr[C_MEM_ADDR_WIDTH + C_MEM_BANKADDR_WIDTH +  C_MEM_NUM_COL_BITS - 1  : C_MEM_BANKADDR_WIDTH +  C_MEM_NUM_COL_BITS ]};


                 if (C_MEM_BANKADDR_WIDTH  == 3 )  //Bank
                          assign p1_cmd_ba = p1_cmd_byte_addr[C_MEM_BANKADDR_WIDTH +  C_MEM_NUM_COL_BITS - 1 :  C_MEM_NUM_COL_BITS ];  //14,3,10
                 else
                          assign p1_cmd_ba = {allzero[2 : C_MEM_BANKADDR_WIDTH],  
                                   p1_cmd_byte_addr[C_MEM_BANKADDR_WIDTH +  C_MEM_NUM_COL_BITS - 1 : C_MEM_NUM_COL_BITS ]};  //14,3,10
                 
                 
                 if (C_MEM_NUM_COL_BITS == 12)  //Column
                          assign p1_cmd_ca[11:0] = p1_cmd_byte_addr[C_MEM_NUM_COL_BITS - 1 : 0];
                 else
                          assign p1_cmd_ca[11:0] = {allzero[11 : C_MEM_NUM_COL_BITS] , p1_cmd_byte_addr[C_MEM_NUM_COL_BITS - 1 : 0]};
                 
                
                // port 2 address remapping
                 if (C_MEM_ADDR_WIDTH == 15)  //Row
                          assign p2_cmd_ra = p2_cmd_byte_addr[C_MEM_ADDR_WIDTH + C_MEM_BANKADDR_WIDTH +  C_MEM_NUM_COL_BITS - 1  : C_MEM_BANKADDR_WIDTH +  C_MEM_NUM_COL_BITS ];
                 else
                          assign p2_cmd_ra = {allzero[14 : C_MEM_ADDR_WIDTH] , p2_cmd_byte_addr[C_MEM_ADDR_WIDTH + C_MEM_BANKADDR_WIDTH +  C_MEM_NUM_COL_BITS - 1  : C_MEM_BANKADDR_WIDTH +  C_MEM_NUM_COL_BITS ]};


                 if (C_MEM_BANKADDR_WIDTH  == 3 )  //Bank
                          assign p2_cmd_ba = p2_cmd_byte_addr[C_MEM_BANKADDR_WIDTH +  C_MEM_NUM_COL_BITS - 1 :  C_MEM_NUM_COL_BITS ];  //14,3,10
                 else
                          assign p2_cmd_ba = {allzero[2 : C_MEM_BANKADDR_WIDTH],  
                                   p2_cmd_byte_addr[C_MEM_BANKADDR_WIDTH +  C_MEM_NUM_COL_BITS - 1 : C_MEM_NUM_COL_BITS ]};  //14,2,10  ***
                 
                 
                 if (C_MEM_NUM_COL_BITS == 12)  //Column
                          assign p2_cmd_ca[11:0] = p2_cmd_byte_addr[C_MEM_NUM_COL_BITS - 1 : 0];
                 else
                          assign p2_cmd_ca[11:0] = {allzero[11 : C_MEM_NUM_COL_BITS] , p2_cmd_byte_addr[C_MEM_NUM_COL_BITS - 1 : 0]};
                 


              //   port 3 address remapping
                 if (C_MEM_ADDR_WIDTH == 15)  //Row
                          assign p3_cmd_ra = p3_cmd_byte_addr[C_MEM_ADDR_WIDTH + C_MEM_BANKADDR_WIDTH +  C_MEM_NUM_COL_BITS - 1  : C_MEM_BANKADDR_WIDTH +  C_MEM_NUM_COL_BITS ];
                 else
                          assign p3_cmd_ra = {allzero[14 : C_MEM_ADDR_WIDTH] , p3_cmd_byte_addr[C_MEM_ADDR_WIDTH + C_MEM_BANKADDR_WIDTH +  C_MEM_NUM_COL_BITS - 1  : C_MEM_BANKADDR_WIDTH +  C_MEM_NUM_COL_BITS ]};


                 if (C_MEM_BANKADDR_WIDTH  == 3 )  //Bank
                          assign p3_cmd_ba = p3_cmd_byte_addr[C_MEM_BANKADDR_WIDTH +  C_MEM_NUM_COL_BITS - 1 :  C_MEM_NUM_COL_BITS ];  //14,3,10
                 else
                          assign p3_cmd_ba = {allzero[2 : C_MEM_BANKADDR_WIDTH],  
                                   p3_cmd_byte_addr[C_MEM_BANKADDR_WIDTH +  C_MEM_NUM_COL_BITS - 1 : C_MEM_NUM_COL_BITS ]};  //14,3,10
                 
                 
                 if (C_MEM_NUM_COL_BITS == 12)  //Column
                          assign p3_cmd_ca[11:0] = p3_cmd_byte_addr[C_MEM_NUM_COL_BITS - 1 : 0];
                 else
                          assign p3_cmd_ca[11:0] = {allzero[11 : C_MEM_NUM_COL_BITS] , p3_cmd_byte_addr[C_MEM_NUM_COL_BITS - 1 : 0]};
                 
                
              //   port 4 address remapping
                 if (C_MEM_ADDR_WIDTH == 15)  //Row
                          assign p4_cmd_ra = p4_cmd_byte_addr[C_MEM_ADDR_WIDTH + C_MEM_BANKADDR_WIDTH +  C_MEM_NUM_COL_BITS - 1  : C_MEM_BANKADDR_WIDTH +  C_MEM_NUM_COL_BITS ];
                 else
                          assign p4_cmd_ra = {allzero[14 : C_MEM_ADDR_WIDTH] , p4_cmd_byte_addr[C_MEM_ADDR_WIDTH + C_MEM_BANKADDR_WIDTH +  C_MEM_NUM_COL_BITS - 1  : C_MEM_BANKADDR_WIDTH +  C_MEM_NUM_COL_BITS ]};


                 if (C_MEM_BANKADDR_WIDTH  == 3 )  //Bank
                          assign p4_cmd_ba = p4_cmd_byte_addr[C_MEM_BANKADDR_WIDTH +  C_MEM_NUM_COL_BITS - 1 :  C_MEM_NUM_COL_BITS ];  //14,3,10
                 else
                          assign p4_cmd_ba = {allzero[2 : C_MEM_BANKADDR_WIDTH],  
                                   p4_cmd_byte_addr[C_MEM_BANKADDR_WIDTH +  C_MEM_NUM_COL_BITS - 1 : C_MEM_NUM_COL_BITS ]};  //14,3,10
                 
                 
                 if (C_MEM_NUM_COL_BITS == 12)  //Column
                          assign p4_cmd_ca[11:0] = p4_cmd_byte_addr[C_MEM_NUM_COL_BITS - 1 : 0];
                 else
                          assign p4_cmd_ca[11:0] = {allzero[11 : C_MEM_NUM_COL_BITS] , p4_cmd_byte_addr[C_MEM_NUM_COL_BITS - 1 : 0]};
                 

              //   port 5 address remapping
              
                 if (C_MEM_ADDR_WIDTH == 15)  //Row
                          assign p5_cmd_ra = p5_cmd_byte_addr[C_MEM_ADDR_WIDTH + C_MEM_BANKADDR_WIDTH +  C_MEM_NUM_COL_BITS - 1  : C_MEM_BANKADDR_WIDTH +  C_MEM_NUM_COL_BITS ];
                 else
                          assign p5_cmd_ra = {allzero[14 : C_MEM_ADDR_WIDTH] , p5_cmd_byte_addr[C_MEM_ADDR_WIDTH + C_MEM_BANKADDR_WIDTH +  C_MEM_NUM_COL_BITS - 1  : C_MEM_BANKADDR_WIDTH +  C_MEM_NUM_COL_BITS ]};


                 if (C_MEM_BANKADDR_WIDTH  == 3 )  //Bank
                          assign p5_cmd_ba = p5_cmd_byte_addr[C_MEM_BANKADDR_WIDTH +  C_MEM_NUM_COL_BITS - 1 :  C_MEM_NUM_COL_BITS ];  //14,3,10
                 else
                          assign p5_cmd_ba = {allzero[2 : C_MEM_BANKADDR_WIDTH],  
                                   p5_cmd_byte_addr[C_MEM_BANKADDR_WIDTH +  C_MEM_NUM_COL_BITS - 1 : C_MEM_NUM_COL_BITS ]};  //14,3,10
                 
                 
                 if (C_MEM_NUM_COL_BITS == 12)  //Column
                          assign p5_cmd_ca[11:0] = p5_cmd_byte_addr[C_MEM_NUM_COL_BITS - 1 : 0];
                 else
                          assign p5_cmd_ca[11:0] = {allzero[11 : C_MEM_NUM_COL_BITS] , p5_cmd_byte_addr[C_MEM_NUM_COL_BITS - 1 : 0]};
                 
                end
               
            else  //  x8 ***************C_MEM_ADDR_ORDER = 0 : Bank Row Column
              begin
                 // port 0 address remapping
                 if (C_MEM_BANKADDR_WIDTH  == 3 ) //Bank
                          assign p0_cmd_ba = p0_cmd_byte_addr[C_MEM_ADDR_WIDTH  +  C_MEM_BANKADDR_WIDTH +  C_MEM_NUM_COL_BITS - 1 : C_MEM_ADDR_WIDTH  +  C_MEM_NUM_COL_BITS ];  
                 else
                          assign p0_cmd_ba = {allzero[2 : C_MEM_BANKADDR_WIDTH],  
                                   p0_cmd_byte_addr[C_MEM_ADDR_WIDTH  +  C_MEM_BANKADDR_WIDTH +  C_MEM_NUM_COL_BITS - 1 : C_MEM_ADDR_WIDTH  +  C_MEM_NUM_COL_BITS ]};  


                 if (C_MEM_ADDR_WIDTH == 15) //Row
                          assign p0_cmd_ra = p0_cmd_byte_addr[C_MEM_ADDR_WIDTH  + C_MEM_NUM_COL_BITS - 1  :  C_MEM_NUM_COL_BITS ];
                 else
                          assign p0_cmd_ra = {allzero[14 : C_MEM_ADDR_WIDTH] , p0_cmd_byte_addr[C_MEM_ADDR_WIDTH  +  C_MEM_NUM_COL_BITS - 1  : C_MEM_NUM_COL_BITS ]};                
                                   
                 
                 if (C_MEM_NUM_COL_BITS == 12) //Column
                          assign p0_cmd_ca[11:0] = p0_cmd_byte_addr[C_MEM_NUM_COL_BITS - 1 : 0];
                 else
                          assign p0_cmd_ca[11:0] = {allzero[11 : C_MEM_NUM_COL_BITS] , p0_cmd_byte_addr[C_MEM_NUM_COL_BITS - 1 : 0]};


                // port 1 address remapping
                 if (C_MEM_BANKADDR_WIDTH  == 3 ) //Bank
                          assign p1_cmd_ba = p1_cmd_byte_addr[C_MEM_ADDR_WIDTH  +  C_MEM_BANKADDR_WIDTH +  C_MEM_NUM_COL_BITS - 1 : C_MEM_ADDR_WIDTH  +  C_MEM_NUM_COL_BITS ];  
                 else
                          assign p1_cmd_ba = {allzero[2 : C_MEM_BANKADDR_WIDTH],  
                                   p1_cmd_byte_addr[C_MEM_ADDR_WIDTH  +  C_MEM_BANKADDR_WIDTH +  C_MEM_NUM_COL_BITS - 1 : C_MEM_ADDR_WIDTH  +  C_MEM_NUM_COL_BITS ]};  
                                   
                 if (C_MEM_ADDR_WIDTH == 15) //Row
                          assign p1_cmd_ra = p1_cmd_byte_addr[C_MEM_ADDR_WIDTH  + C_MEM_NUM_COL_BITS - 1  :  C_MEM_NUM_COL_BITS ];
                 else
                          assign p1_cmd_ra = {allzero[14 : C_MEM_ADDR_WIDTH] , p1_cmd_byte_addr[C_MEM_ADDR_WIDTH  +  C_MEM_NUM_COL_BITS - 1  : C_MEM_NUM_COL_BITS ]};
                 
                 if (C_MEM_NUM_COL_BITS == 12) //Column
                          assign p1_cmd_ca[11:0] = p1_cmd_byte_addr[C_MEM_NUM_COL_BITS - 1 : 0];
                 else
                          assign p1_cmd_ca[11:0] = {allzero[11 : C_MEM_NUM_COL_BITS] , p1_cmd_byte_addr[C_MEM_NUM_COL_BITS - 1 : 0]};
                
               //port 2 address remapping
                if (C_MEM_BANKADDR_WIDTH  == 3 ) //Bank    2,13,10    24,23
                       assign p2_cmd_ba = p2_cmd_byte_addr[C_MEM_ADDR_WIDTH  +  C_MEM_BANKADDR_WIDTH +  C_MEM_NUM_COL_BITS - 1 : C_MEM_ADDR_WIDTH  +  C_MEM_NUM_COL_BITS ];  
                else
                       assign p2_cmd_ba = {allzero[2 : C_MEM_BANKADDR_WIDTH],  
                                        p2_cmd_byte_addr[C_MEM_ADDR_WIDTH  +  C_MEM_BANKADDR_WIDTH +  C_MEM_NUM_COL_BITS - 1 : C_MEM_ADDR_WIDTH  +  C_MEM_NUM_COL_BITS  ]};  
      
                 if (C_MEM_ADDR_WIDTH == 15) //Row
                          assign p2_cmd_ra = p2_cmd_byte_addr[C_MEM_ADDR_WIDTH  + C_MEM_NUM_COL_BITS - 1  :  C_MEM_NUM_COL_BITS ];
                 else
                          assign p2_cmd_ra = {allzero[14 : C_MEM_ADDR_WIDTH] , p2_cmd_byte_addr[C_MEM_ADDR_WIDTH  +  C_MEM_NUM_COL_BITS - 1  : C_MEM_NUM_COL_BITS ]};
                 
                 if (C_MEM_NUM_COL_BITS == 12) //Column
                          assign p2_cmd_ca[11:0] = p2_cmd_byte_addr[C_MEM_NUM_COL_BITS - 1 : 0];
                 else
                          assign p2_cmd_ca[11:0] = {allzero[11 : C_MEM_NUM_COL_BITS] , p2_cmd_byte_addr[C_MEM_NUM_COL_BITS - 1 : 0]};

              // port 3 address remapping
                 if (C_MEM_BANKADDR_WIDTH  == 3 ) //Bank
                          assign p3_cmd_ba = p3_cmd_byte_addr[C_MEM_ADDR_WIDTH  +  C_MEM_BANKADDR_WIDTH +  C_MEM_NUM_COL_BITS - 1 : C_MEM_ADDR_WIDTH  +  C_MEM_NUM_COL_BITS ];  
                 else
                          assign p3_cmd_ba = {allzero[2 : C_MEM_BANKADDR_WIDTH],  
                                   p3_cmd_byte_addr[C_MEM_ADDR_WIDTH  +  C_MEM_BANKADDR_WIDTH +  C_MEM_NUM_COL_BITS - 1 : C_MEM_ADDR_WIDTH  +  C_MEM_NUM_COL_BITS ]};  
                                   
                 if (C_MEM_ADDR_WIDTH == 15) //Row
                          assign p3_cmd_ra = p3_cmd_byte_addr[C_MEM_ADDR_WIDTH  + C_MEM_NUM_COL_BITS - 1  :  C_MEM_NUM_COL_BITS ];
                 else
                          assign p3_cmd_ra = {allzero[14 : C_MEM_ADDR_WIDTH] , p3_cmd_byte_addr[C_MEM_ADDR_WIDTH  +  C_MEM_NUM_COL_BITS - 1  : C_MEM_NUM_COL_BITS ]};
                 
                 if (C_MEM_NUM_COL_BITS == 12) //Column
                          assign p3_cmd_ca[11:0] = p3_cmd_byte_addr[C_MEM_NUM_COL_BITS - 1 : 0];
                 else
                          assign p3_cmd_ca[11:0] = {allzero[11 : C_MEM_NUM_COL_BITS] , p3_cmd_byte_addr[C_MEM_NUM_COL_BITS - 1 : 0]};
   
   
                 //   port 4 address remapping
                 if (C_MEM_BANKADDR_WIDTH  == 3 ) //Bank
                          assign p4_cmd_ba = p4_cmd_byte_addr[C_MEM_ADDR_WIDTH  +  C_MEM_BANKADDR_WIDTH +  C_MEM_NUM_COL_BITS - 1 : C_MEM_ADDR_WIDTH  +  C_MEM_NUM_COL_BITS ];  
                 else
                          assign p4_cmd_ba = {allzero[2 : C_MEM_BANKADDR_WIDTH],  
                                   p4_cmd_byte_addr[C_MEM_ADDR_WIDTH  +  C_MEM_BANKADDR_WIDTH +  C_MEM_NUM_COL_BITS - 1 : C_MEM_ADDR_WIDTH  +  C_MEM_NUM_COL_BITS ]};  
                                   
                 if (C_MEM_ADDR_WIDTH == 15) //Row
                          assign p4_cmd_ra = p4_cmd_byte_addr[C_MEM_ADDR_WIDTH  + C_MEM_NUM_COL_BITS - 1  :  C_MEM_NUM_COL_BITS ];
                 else
                          assign p4_cmd_ra = {allzero[14 : C_MEM_ADDR_WIDTH] , p4_cmd_byte_addr[C_MEM_ADDR_WIDTH  +  C_MEM_NUM_COL_BITS - 1  : C_MEM_NUM_COL_BITS ]};
                 
                 if (C_MEM_NUM_COL_BITS == 12) //Column
                          assign p4_cmd_ca[11:0] = p4_cmd_byte_addr[C_MEM_NUM_COL_BITS - 1 : 0];
                 else
                          assign p4_cmd_ca[11:0] = {allzero[11 : C_MEM_NUM_COL_BITS] , p4_cmd_byte_addr[C_MEM_NUM_COL_BITS - 1 : 0]};

                 //   port 5 address remapping
   
                 if (C_MEM_BANKADDR_WIDTH  == 3 ) //Bank
                          assign p5_cmd_ba = p5_cmd_byte_addr[C_MEM_ADDR_WIDTH  +  C_MEM_BANKADDR_WIDTH +  C_MEM_NUM_COL_BITS - 1 : C_MEM_ADDR_WIDTH  +  C_MEM_NUM_COL_BITS ];  
                 else
                          assign p5_cmd_ba = {allzero[2 : C_MEM_BANKADDR_WIDTH],  
                                   p5_cmd_byte_addr[C_MEM_ADDR_WIDTH  +  C_MEM_BANKADDR_WIDTH +  C_MEM_NUM_COL_BITS - 1 : C_MEM_ADDR_WIDTH  +  C_MEM_NUM_COL_BITS ]};  
                                   
                 if (C_MEM_ADDR_WIDTH == 15) //Row
                          assign p5_cmd_ra = p5_cmd_byte_addr[C_MEM_ADDR_WIDTH  + C_MEM_NUM_COL_BITS - 1  :  C_MEM_NUM_COL_BITS ];
                 else
                          assign p5_cmd_ra = {allzero[14 : C_MEM_ADDR_WIDTH] , p5_cmd_byte_addr[C_MEM_ADDR_WIDTH  +  C_MEM_NUM_COL_BITS - 1  : C_MEM_NUM_COL_BITS ]};
                 
                 if (C_MEM_NUM_COL_BITS == 12) //Column
                          assign p5_cmd_ca[11:0] = p5_cmd_byte_addr[C_MEM_NUM_COL_BITS - 1 : 0];
                 else
                          assign p5_cmd_ca[11:0] = {allzero[11 : C_MEM_NUM_COL_BITS] , p5_cmd_byte_addr[C_MEM_NUM_COL_BITS - 1 : 0]};
             
            end

              //

end else if(C_NUM_DQ_PINS == 4) begin : x4_Addr

           if (C_MEM_ADDR_ORDER == "ROW_BANK_COLUMN") begin  // C_MEM_ADDR_ORDER = 1 :  Row Bank Column

               //   port 0 address remapping
               
               
               if (C_MEM_ADDR_WIDTH == 15) //Row
                     assign p0_cmd_ra = p0_cmd_byte_addr[C_MEM_ADDR_WIDTH + C_MEM_BANKADDR_WIDTH +  C_MEM_NUM_COL_BITS - 2 : C_MEM_BANKADDR_WIDTH + C_MEM_NUM_COL_BITS - 1];
               else         
                     assign p0_cmd_ra = {allzero[14 : C_MEM_ADDR_WIDTH ] , p0_cmd_byte_addr[C_MEM_ADDR_WIDTH +  C_MEM_BANKADDR_WIDTH + C_MEM_NUM_COL_BITS - 2 : C_MEM_BANKADDR_WIDTH + C_MEM_NUM_COL_BITS - 1]};
                        

               if (C_MEM_BANKADDR_WIDTH  == 3 ) //Bank
                      assign p0_cmd_ba =  p0_cmd_byte_addr[C_MEM_BANKADDR_WIDTH + C_MEM_NUM_COL_BITS - 2 :  C_MEM_NUM_COL_BITS - 1];
               else
                      assign p0_cmd_ba = {allzero[2 : C_MEM_BANKADDR_WIDTH ] , p0_cmd_byte_addr[C_MEM_BANKADDR_WIDTH + C_MEM_NUM_COL_BITS - 2 :  C_MEM_NUM_COL_BITS - 1]};

                        
               if (C_MEM_NUM_COL_BITS == 12) //Column
                     assign p0_cmd_ca = {p0_cmd_byte_addr[C_MEM_NUM_COL_BITS - 2 : 0] , 1'b0};                                //14,3,11
               else
                     assign p0_cmd_ca = {allzero[11 : C_MEM_NUM_COL_BITS ] ,  p0_cmd_byte_addr[C_MEM_NUM_COL_BITS - 2 : 0] , 1'b0};

           
              //   port 1 address remapping
               if (C_MEM_ADDR_WIDTH == 15) //Row
                     assign p1_cmd_ra = p1_cmd_byte_addr[C_MEM_ADDR_WIDTH + C_MEM_BANKADDR_WIDTH +  C_MEM_NUM_COL_BITS - 2 : C_MEM_BANKADDR_WIDTH + C_MEM_NUM_COL_BITS - 1];
               else         
                     assign p1_cmd_ra = {allzero[14 : C_MEM_ADDR_WIDTH ] , p1_cmd_byte_addr[C_MEM_ADDR_WIDTH +  C_MEM_BANKADDR_WIDTH + C_MEM_NUM_COL_BITS - 2 : C_MEM_BANKADDR_WIDTH + C_MEM_NUM_COL_BITS - 1]};
                        

               if (C_MEM_BANKADDR_WIDTH  == 3 ) //Bank
                      assign p1_cmd_ba =  p1_cmd_byte_addr[C_MEM_BANKADDR_WIDTH + C_MEM_NUM_COL_BITS - 2 :  C_MEM_NUM_COL_BITS - 1];
               else
                      assign p1_cmd_ba = {allzero[2 : C_MEM_BANKADDR_WIDTH ] , p1_cmd_byte_addr[C_MEM_BANKADDR_WIDTH + C_MEM_NUM_COL_BITS - 2 :  C_MEM_NUM_COL_BITS - 1]};

                        
               if (C_MEM_NUM_COL_BITS == 12) //Column
                     assign p1_cmd_ca = {p1_cmd_byte_addr[C_MEM_NUM_COL_BITS - 2 : 0] , 1'b0};                                //14,3,11
               else
                     assign p1_cmd_ca = {allzero[11 : C_MEM_NUM_COL_BITS ] ,  p1_cmd_byte_addr[C_MEM_NUM_COL_BITS - 2 : 0] , 1'b0};

               //   port 2 address remapping
               if (C_MEM_ADDR_WIDTH == 15) //Row
                     assign p2_cmd_ra = p2_cmd_byte_addr[C_MEM_ADDR_WIDTH + C_MEM_BANKADDR_WIDTH +  C_MEM_NUM_COL_BITS - 2 : C_MEM_BANKADDR_WIDTH + C_MEM_NUM_COL_BITS - 1];
               else         
                     assign p2_cmd_ra = {allzero[14 : C_MEM_ADDR_WIDTH ] , p2_cmd_byte_addr[C_MEM_ADDR_WIDTH +  C_MEM_BANKADDR_WIDTH + C_MEM_NUM_COL_BITS - 2 : C_MEM_BANKADDR_WIDTH + C_MEM_NUM_COL_BITS - 1]};
                        

               if (C_MEM_BANKADDR_WIDTH  == 3 ) //Bank
                      assign p2_cmd_ba =  p2_cmd_byte_addr[C_MEM_BANKADDR_WIDTH + C_MEM_NUM_COL_BITS - 2 :  C_MEM_NUM_COL_BITS - 1];
               else
                      assign p2_cmd_ba = {allzero[2 : C_MEM_BANKADDR_WIDTH ] , p2_cmd_byte_addr[C_MEM_BANKADDR_WIDTH + C_MEM_NUM_COL_BITS - 2 :  C_MEM_NUM_COL_BITS - 1]};

                        
               if (C_MEM_NUM_COL_BITS == 12) //Column
                     assign p2_cmd_ca = {p2_cmd_byte_addr[C_MEM_NUM_COL_BITS - 2 : 0] , 1'b0};                                //14,3,11
               else
                     assign p2_cmd_ca = {allzero[11 : C_MEM_NUM_COL_BITS ] ,  p2_cmd_byte_addr[C_MEM_NUM_COL_BITS - 2 : 0] , 1'b0};

              //   port 3 address remapping

               if (C_MEM_ADDR_WIDTH == 15) //Row
                     assign p3_cmd_ra = p3_cmd_byte_addr[C_MEM_ADDR_WIDTH + C_MEM_BANKADDR_WIDTH +  C_MEM_NUM_COL_BITS - 2 : C_MEM_BANKADDR_WIDTH + C_MEM_NUM_COL_BITS - 1];
               else         
                     assign p3_cmd_ra = {allzero[14 : C_MEM_ADDR_WIDTH ] , p3_cmd_byte_addr[C_MEM_ADDR_WIDTH +  C_MEM_BANKADDR_WIDTH + C_MEM_NUM_COL_BITS - 2 : C_MEM_BANKADDR_WIDTH + C_MEM_NUM_COL_BITS - 1]};
                        

               if (C_MEM_BANKADDR_WIDTH  == 3 ) //Bank
                      assign p3_cmd_ba =  p3_cmd_byte_addr[C_MEM_BANKADDR_WIDTH + C_MEM_NUM_COL_BITS - 2 :  C_MEM_NUM_COL_BITS - 1];
               else
                      assign p3_cmd_ba = {allzero[2 : C_MEM_BANKADDR_WIDTH ] , p3_cmd_byte_addr[C_MEM_BANKADDR_WIDTH + C_MEM_NUM_COL_BITS - 2 :  C_MEM_NUM_COL_BITS - 1]};

                        
               if (C_MEM_NUM_COL_BITS == 12) //Column
                     assign p3_cmd_ca = {p3_cmd_byte_addr[C_MEM_NUM_COL_BITS - 2 : 0] , 1'b0};                                //14,3,11
               else
                     assign p3_cmd_ca = {allzero[11 : C_MEM_NUM_COL_BITS ] ,  p3_cmd_byte_addr[C_MEM_NUM_COL_BITS - 2 : 0] , 1'b0};

 

          if(C_PORT_CONFIG == "B32_B32_R32_R32_R32_R32" ||
             C_PORT_CONFIG == "B32_B32_R32_R32_R32_W32" ||
             C_PORT_CONFIG == "B32_B32_R32_R32_W32_R32" ||
             C_PORT_CONFIG == "B32_B32_R32_R32_W32_W32" ||
             C_PORT_CONFIG == "B32_B32_R32_W32_R32_R32" ||
             C_PORT_CONFIG == "B32_B32_R32_W32_R32_W32" ||
             C_PORT_CONFIG == "B32_B32_R32_W32_W32_R32" ||
             C_PORT_CONFIG == "B32_B32_R32_W32_W32_W32" ||
             C_PORT_CONFIG == "B32_B32_W32_R32_R32_R32" ||
             C_PORT_CONFIG == "B32_B32_W32_R32_R32_W32" ||
             C_PORT_CONFIG == "B32_B32_W32_R32_W32_R32" ||
             C_PORT_CONFIG == "B32_B32_W32_R32_W32_W32" ||
             C_PORT_CONFIG == "B32_B32_W32_W32_R32_R32" ||
             C_PORT_CONFIG == "B32_B32_W32_W32_R32_W32" ||
             C_PORT_CONFIG == "B32_B32_W32_W32_W32_R32" ||
             C_PORT_CONFIG == "B32_B32_W32_W32_W32_W32"
             ) //begin : x4_Addr_CFG1_OR_CFG2
               begin
               if (C_MEM_ADDR_WIDTH == 15) //Row
                     assign p4_cmd_ra = p4_cmd_byte_addr[C_MEM_ADDR_WIDTH + C_MEM_BANKADDR_WIDTH +  C_MEM_NUM_COL_BITS - 2 : C_MEM_BANKADDR_WIDTH + C_MEM_NUM_COL_BITS - 1];
               else         
                     assign p4_cmd_ra = {allzero[14 : C_MEM_ADDR_WIDTH ] , p4_cmd_byte_addr[C_MEM_ADDR_WIDTH +  C_MEM_BANKADDR_WIDTH + C_MEM_NUM_COL_BITS - 2 : C_MEM_BANKADDR_WIDTH + C_MEM_NUM_COL_BITS - 1]};
                        

               if (C_MEM_BANKADDR_WIDTH  == 3 ) //Bank
                      assign p4_cmd_ba =  p4_cmd_byte_addr[C_MEM_BANKADDR_WIDTH + C_MEM_NUM_COL_BITS - 2 :  C_MEM_NUM_COL_BITS - 1];
               else
                      assign p4_cmd_ba = {allzero[2 : C_MEM_BANKADDR_WIDTH ] , p4_cmd_byte_addr[C_MEM_BANKADDR_WIDTH + C_MEM_NUM_COL_BITS - 2 :  C_MEM_NUM_COL_BITS - 1]};

                        
               if (C_MEM_NUM_COL_BITS == 12) //Column
                     assign p4_cmd_ca = {p4_cmd_byte_addr[C_MEM_NUM_COL_BITS - 2 : 0] , 1'b0};                                //14,3,11
               else
                     assign p4_cmd_ca = {allzero[11 : C_MEM_NUM_COL_BITS ] ,  p4_cmd_byte_addr[C_MEM_NUM_COL_BITS - 2 : 0] , 1'b0};



               if (C_MEM_ADDR_WIDTH == 15) //Row
                     assign p5_cmd_ra = p5_cmd_byte_addr[C_MEM_ADDR_WIDTH + C_MEM_BANKADDR_WIDTH +  C_MEM_NUM_COL_BITS - 2 : C_MEM_BANKADDR_WIDTH + C_MEM_NUM_COL_BITS - 1];
               else         
                     assign p5_cmd_ra = {allzero[14 : C_MEM_ADDR_WIDTH ] , p5_cmd_byte_addr[C_MEM_ADDR_WIDTH +  C_MEM_BANKADDR_WIDTH + C_MEM_NUM_COL_BITS - 2 : C_MEM_BANKADDR_WIDTH + C_MEM_NUM_COL_BITS - 1]};
                        

               if (C_MEM_BANKADDR_WIDTH  == 3 ) //Bank
                      assign p5_cmd_ba =  p5_cmd_byte_addr[C_MEM_BANKADDR_WIDTH + C_MEM_NUM_COL_BITS - 2 :  C_MEM_NUM_COL_BITS - 1];
               else
                      assign p5_cmd_ba = {allzero[2 : C_MEM_BANKADDR_WIDTH ] , p5_cmd_byte_addr[C_MEM_BANKADDR_WIDTH + C_MEM_NUM_COL_BITS - 2 :  C_MEM_NUM_COL_BITS - 1]};

                        
               if (C_MEM_NUM_COL_BITS == 12) //Column
                     assign p5_cmd_ca = {p5_cmd_byte_addr[C_MEM_NUM_COL_BITS - 2 : 0] , 1'b0};                                //14,3,11
               else
                     assign p5_cmd_ca = {allzero[11 : C_MEM_NUM_COL_BITS ] ,  p5_cmd_byte_addr[C_MEM_NUM_COL_BITS - 2 : 0] , 1'b0};

              end
              
              
           end
         else   // C_MEM_ADDR_ORDER = 1 :  Row Bank Column
            begin
            
               //   port 0 address remapping
               if (C_MEM_BANKADDR_WIDTH  == 3 ) //Bank
                      assign p0_cmd_ba =  p0_cmd_byte_addr[C_MEM_BANKADDR_WIDTH + C_MEM_ADDR_WIDTH +  C_MEM_NUM_COL_BITS - 2 : C_MEM_ADDR_WIDTH +  C_MEM_NUM_COL_BITS - 1];
               else
                      assign p0_cmd_ba = {allzero[2 : C_MEM_BANKADDR_WIDTH ] , p0_cmd_byte_addr[C_MEM_BANKADDR_WIDTH + C_MEM_ADDR_WIDTH +  C_MEM_NUM_COL_BITS - 2 : C_MEM_ADDR_WIDTH +  C_MEM_NUM_COL_BITS - 1]};
               
               
               if (C_MEM_ADDR_WIDTH == 15) //Row
                     assign p0_cmd_ra = p0_cmd_byte_addr[C_MEM_ADDR_WIDTH +  C_MEM_NUM_COL_BITS - 2 : C_MEM_NUM_COL_BITS - 1];
               else         
                     assign p0_cmd_ra = {allzero[14 : C_MEM_ADDR_WIDTH ] , p0_cmd_byte_addr[C_MEM_ADDR_WIDTH +  C_MEM_NUM_COL_BITS - 2 : C_MEM_NUM_COL_BITS - 1]};
                        
                        
               if (C_MEM_NUM_COL_BITS == 12) //Column
                     assign p0_cmd_ca = {p0_cmd_byte_addr[C_MEM_NUM_COL_BITS - 2 : 0] , 1'b0};
               else
                     assign p0_cmd_ca = {allzero[11 : C_MEM_NUM_COL_BITS ] ,  p0_cmd_byte_addr[C_MEM_NUM_COL_BITS - 2 : 0] , 1'b0};
               
           
              //   port 1 address remapping
               if (C_MEM_BANKADDR_WIDTH  == 3 ) //Bank
                      assign p1_cmd_ba =  p1_cmd_byte_addr[C_MEM_BANKADDR_WIDTH + C_MEM_ADDR_WIDTH +  C_MEM_NUM_COL_BITS - 2 : C_MEM_ADDR_WIDTH +  C_MEM_NUM_COL_BITS - 1];
               else
                      assign p1_cmd_ba = {allzero[2 : C_MEM_BANKADDR_WIDTH ] , p1_cmd_byte_addr[C_MEM_BANKADDR_WIDTH + C_MEM_ADDR_WIDTH +  C_MEM_NUM_COL_BITS - 2 : C_MEM_ADDR_WIDTH +  C_MEM_NUM_COL_BITS - 1]};
               
               
               if (C_MEM_ADDR_WIDTH == 15) //Row
                     assign p1_cmd_ra = p1_cmd_byte_addr[C_MEM_ADDR_WIDTH +  C_MEM_NUM_COL_BITS - 2 : C_MEM_NUM_COL_BITS - 1];
               else         
                     assign p1_cmd_ra = {allzero[14 : C_MEM_ADDR_WIDTH ] , p1_cmd_byte_addr[C_MEM_ADDR_WIDTH +  C_MEM_NUM_COL_BITS - 2 : C_MEM_NUM_COL_BITS - 1]};
                        
                        
               if (C_MEM_NUM_COL_BITS == 12) //Column
                     assign p1_cmd_ca = {p1_cmd_byte_addr[C_MEM_NUM_COL_BITS - 2 : 0] , 1'b0};
               else
                     assign p1_cmd_ca = {allzero[11 : C_MEM_NUM_COL_BITS ] ,  p1_cmd_byte_addr[C_MEM_NUM_COL_BITS - 2 : 0] , 1'b0};
               //   port 2 address remapping
               if (C_MEM_BANKADDR_WIDTH  == 3 ) //Bank
                      assign p2_cmd_ba =  p2_cmd_byte_addr[C_MEM_BANKADDR_WIDTH + C_MEM_ADDR_WIDTH +  C_MEM_NUM_COL_BITS - 2 : C_MEM_ADDR_WIDTH +  C_MEM_NUM_COL_BITS - 1];
               else
                      assign p2_cmd_ba = {allzero[2 : C_MEM_BANKADDR_WIDTH ] , p2_cmd_byte_addr[C_MEM_BANKADDR_WIDTH + C_MEM_ADDR_WIDTH +  C_MEM_NUM_COL_BITS - 2 : C_MEM_ADDR_WIDTH +  C_MEM_NUM_COL_BITS - 1]};
               
             //***  
               if (C_MEM_ADDR_WIDTH == 15) //Row
                     assign p2_cmd_ra = p2_cmd_byte_addr[C_MEM_ADDR_WIDTH +  C_MEM_NUM_COL_BITS - 2 : C_MEM_NUM_COL_BITS - 1];
               else         
                     assign p2_cmd_ra = {allzero[14 : C_MEM_ADDR_WIDTH ] , p2_cmd_byte_addr[C_MEM_ADDR_WIDTH +  C_MEM_NUM_COL_BITS - 2 : C_MEM_NUM_COL_BITS - 1]};
                        
                        
               if (C_MEM_NUM_COL_BITS == 12) //Column
                     assign p2_cmd_ca = {p2_cmd_byte_addr[C_MEM_NUM_COL_BITS - 2 : 0] , 1'b0};
               else
                     assign p2_cmd_ca = {allzero[11 : C_MEM_NUM_COL_BITS ] ,  p2_cmd_byte_addr[C_MEM_NUM_COL_BITS - 2 : 0] , 1'b0};
              //   port 3 address remapping

               if (C_MEM_BANKADDR_WIDTH  == 3 ) //Bank
                      assign p3_cmd_ba =  p3_cmd_byte_addr[C_MEM_BANKADDR_WIDTH + C_MEM_ADDR_WIDTH +  C_MEM_NUM_COL_BITS - 2 : C_MEM_ADDR_WIDTH +  C_MEM_NUM_COL_BITS - 1];
               else
                      assign p3_cmd_ba = {allzero[2 : C_MEM_BANKADDR_WIDTH ] , p3_cmd_byte_addr[C_MEM_BANKADDR_WIDTH + C_MEM_ADDR_WIDTH +  C_MEM_NUM_COL_BITS - 2 : C_MEM_ADDR_WIDTH +  C_MEM_NUM_COL_BITS - 1]};
               
               
               if (C_MEM_ADDR_WIDTH == 15) //Row
                     assign p3_cmd_ra = p3_cmd_byte_addr[C_MEM_ADDR_WIDTH +  C_MEM_NUM_COL_BITS - 2 : C_MEM_NUM_COL_BITS - 1];
               else         
                     assign p3_cmd_ra = {allzero[14 : C_MEM_ADDR_WIDTH ] , p3_cmd_byte_addr[C_MEM_ADDR_WIDTH +  C_MEM_NUM_COL_BITS - 2 : C_MEM_NUM_COL_BITS - 1]};
                        
                        
               if (C_MEM_NUM_COL_BITS == 12) //Column
                     assign p3_cmd_ca = {p3_cmd_byte_addr[C_MEM_NUM_COL_BITS - 2 : 0] , 1'b0};
               else
                     assign p3_cmd_ca = {allzero[11 : C_MEM_NUM_COL_BITS ] ,  p3_cmd_byte_addr[C_MEM_NUM_COL_BITS - 2 : 0] , 1'b0};
 

          if(C_PORT_CONFIG == "B32_B32_R32_R32_R32_R32" ||
             C_PORT_CONFIG == "B32_B32_R32_R32_R32_W32" ||
             C_PORT_CONFIG == "B32_B32_R32_R32_W32_R32" ||
             C_PORT_CONFIG == "B32_B32_R32_R32_W32_W32" ||
             C_PORT_CONFIG == "B32_B32_R32_W32_R32_R32" ||
             C_PORT_CONFIG == "B32_B32_R32_W32_R32_W32" ||
             C_PORT_CONFIG == "B32_B32_R32_W32_W32_R32" ||
             C_PORT_CONFIG == "B32_B32_R32_W32_W32_W32" ||
             C_PORT_CONFIG == "B32_B32_W32_R32_R32_R32" ||
             C_PORT_CONFIG == "B32_B32_W32_R32_R32_W32" ||
             C_PORT_CONFIG == "B32_B32_W32_R32_W32_R32" ||
             C_PORT_CONFIG == "B32_B32_W32_R32_W32_W32" ||
             C_PORT_CONFIG == "B32_B32_W32_W32_R32_R32" ||
             C_PORT_CONFIG == "B32_B32_W32_W32_R32_W32" ||
             C_PORT_CONFIG == "B32_B32_W32_W32_W32_R32" ||
             C_PORT_CONFIG == "B32_B32_W32_W32_W32_W32"
             ) //begin : x4_Addr_CFG1_OR_CFG2
               begin
               if (C_MEM_BANKADDR_WIDTH  == 3 ) //Bank
                      assign p4_cmd_ba =  p4_cmd_byte_addr[C_MEM_BANKADDR_WIDTH + C_MEM_ADDR_WIDTH +  C_MEM_NUM_COL_BITS - 2 : C_MEM_ADDR_WIDTH +  C_MEM_NUM_COL_BITS - 1];
               else
                      assign p4_cmd_ba = {allzero[2 : C_MEM_BANKADDR_WIDTH ] , p4_cmd_byte_addr[C_MEM_BANKADDR_WIDTH + C_MEM_ADDR_WIDTH +  C_MEM_NUM_COL_BITS - 2 : C_MEM_ADDR_WIDTH +  C_MEM_NUM_COL_BITS - 1]};
               
               
               if (C_MEM_ADDR_WIDTH == 15) //Row
                     assign p4_cmd_ra = p4_cmd_byte_addr[C_MEM_ADDR_WIDTH +  C_MEM_NUM_COL_BITS - 2 : C_MEM_NUM_COL_BITS - 1];
               else         
                     assign p4_cmd_ra = {allzero[14 : C_MEM_ADDR_WIDTH ] , p4_cmd_byte_addr[C_MEM_ADDR_WIDTH +  C_MEM_NUM_COL_BITS - 2 : C_MEM_NUM_COL_BITS - 1]};
                        
                        
               if (C_MEM_NUM_COL_BITS == 12) //Column
                     assign p4_cmd_ca = {p4_cmd_byte_addr[C_MEM_NUM_COL_BITS - 2 : 0] , 1'b0};
               else
                     assign p4_cmd_ca = {allzero[11 : C_MEM_NUM_COL_BITS ] ,  p4_cmd_byte_addr[C_MEM_NUM_COL_BITS - 2 : 0] , 1'b0};


               if (C_MEM_BANKADDR_WIDTH  == 3 ) //Bank
                      assign p5_cmd_ba =  p5_cmd_byte_addr[C_MEM_BANKADDR_WIDTH + C_MEM_ADDR_WIDTH +  C_MEM_NUM_COL_BITS - 2 : C_MEM_ADDR_WIDTH +  C_MEM_NUM_COL_BITS - 1];
               else
                      assign p5_cmd_ba = {allzero[2 : C_MEM_BANKADDR_WIDTH ] , p5_cmd_byte_addr[C_MEM_BANKADDR_WIDTH + C_MEM_ADDR_WIDTH +  C_MEM_NUM_COL_BITS - 2 : C_MEM_ADDR_WIDTH +  C_MEM_NUM_COL_BITS - 1]};
               
               
               if (C_MEM_ADDR_WIDTH == 15) //Row
                     assign p5_cmd_ra = p5_cmd_byte_addr[C_MEM_ADDR_WIDTH +  C_MEM_NUM_COL_BITS - 2 : C_MEM_NUM_COL_BITS - 1];
               else         
                     assign p5_cmd_ra = {allzero[14 : C_MEM_ADDR_WIDTH ] , p5_cmd_byte_addr[C_MEM_ADDR_WIDTH +  C_MEM_NUM_COL_BITS - 2 : C_MEM_NUM_COL_BITS - 1]};
                        
                        
               if (C_MEM_NUM_COL_BITS == 12) //Column
                     assign p5_cmd_ca = {p5_cmd_byte_addr[C_MEM_NUM_COL_BITS - 2 : 0] , 1'b0};
               else
                     assign p5_cmd_ca = {allzero[11 : C_MEM_NUM_COL_BITS ] ,  p5_cmd_byte_addr[C_MEM_NUM_COL_BITS - 2 : 0] , 1'b0};
              end
            
            
            
            end
           
end // block: x4_Addr


endgenerate



generate 
   //   if(C_PORT_CONFIG[183:160] == "B32") begin : u_config1_0
   if(C_PORT_CONFIG == "B32_B32_R32_R32_R32_R32" ||
      C_PORT_CONFIG == "B32_B32_R32_R32_R32_W32" ||
      C_PORT_CONFIG == "B32_B32_R32_R32_W32_R32" ||
      C_PORT_CONFIG == "B32_B32_R32_R32_W32_W32" ||
      C_PORT_CONFIG == "B32_B32_R32_W32_R32_R32" ||
      C_PORT_CONFIG == "B32_B32_R32_W32_R32_W32" ||
      C_PORT_CONFIG == "B32_B32_R32_W32_W32_R32" ||
      C_PORT_CONFIG == "B32_B32_R32_W32_W32_W32" ||
      C_PORT_CONFIG == "B32_B32_W32_R32_R32_R32" ||
      C_PORT_CONFIG == "B32_B32_W32_R32_R32_W32" ||
      C_PORT_CONFIG == "B32_B32_W32_R32_W32_R32" ||
      C_PORT_CONFIG == "B32_B32_W32_R32_W32_W32" ||
      C_PORT_CONFIG == "B32_B32_W32_W32_R32_R32" ||
      C_PORT_CONFIG == "B32_B32_W32_W32_R32_W32" ||
      C_PORT_CONFIG == "B32_B32_W32_W32_W32_R32" ||
      C_PORT_CONFIG == "B32_B32_W32_W32_W32_W32"
      ) begin : u_config1_0      

  //synthesis translate_off 
  always @(*)
  begin
    if ( C_PORT_CONFIG[119:96]  == "W32" && p2_cmd_en == 1'b1 
         && p2_cmd_instr[2] == 1'b0 && p2_cmd_instr[0] == 1'b1 )
          begin
           $display("ERROR - Invalid Command for write only port 2");
           $finish;
          end
  end
              
  always @(*)
  begin
    if ( C_PORT_CONFIG[119:96]  == "R32" && p2_cmd_en == 1'b1 
         && p2_cmd_instr[2] == 1'b0 && p2_cmd_instr[0] == 1'b0 )
          begin
           $display("ERROR - Invalid Command for read only port 2");
           $finish;
          end
  end
// Catch Invalid command during simulation for Port 3              
  always @(*)
  begin
    if ( C_PORT_CONFIG[87:64]  == "W32" && p3_cmd_en == 1'b1 
         && p3_cmd_instr[2] == 1'b0 && p3_cmd_instr[0] == 1'b1 )
          begin
           $display("ERROR - Invalid Command for write only port 3");
           $finish;
          end
  end
              
  always @(*)
  begin
    if ( C_PORT_CONFIG[87:64]  == "R32" && p3_cmd_en == 1'b1 
         && p3_cmd_instr[2] == 1'b0  && p3_cmd_instr[0] == 1'b0 )
          begin
           $display("ERROR - Invalid Command for read only port 3");
           $finish;
          end
  end
  
// Catch Invalid command during simulation for Port 4              
  always @(*)
  begin
    if ( C_PORT_CONFIG[55:32]  == "W32" && p4_cmd_en == 1'b1 
         && p4_cmd_instr[2] == 1'b0 && p4_cmd_instr[0] == 1'b1 )
          begin
           $display("ERROR - Invalid Command for write only port 4");
           $finish;
          end
  end
              
  always @(*)
  begin
    if ( C_PORT_CONFIG[55:32]  == "R32" && p4_cmd_en == 1'b1 
         && p4_cmd_instr[2] == 1'b0 && p4_cmd_instr[0] == 1'b0 )
          begin
           $display("ERROR - Invalid Command for read only port 4");
           $finish;
          end
  end
// Catch Invalid command during simulation for Port 5              
  always @(*)
  begin
    if ( C_PORT_CONFIG[23:0]  == "W32" && p5_cmd_en == 1'b1 
         && p5_cmd_instr[2] == 1'b0 && p5_cmd_instr[0] == 1'b1 )
          begin
           $display("ERROR - Invalid Command for write only port 5");
           $finish;
          end
  end
              
  always @(*)
  begin
    if ( C_PORT_CONFIG[23:0]  == "R32" && p5_cmd_en == 1'b1 
         && p5_cmd_instr[2] == 1'b0  && p5_cmd_instr[0] == 1'b0 )
          begin
           $display("ERROR - Invalid Command for read only port 5");
           $finish;
          end
  end  
   //synthesis translate_on 


  // the local declaration of input port signals doesn't work.  The mig_p1_xxx through mig_p5_xxx always ends up
  // high Z even though there are signals on p1_cmd_xxx through p5_cmd_xxxx.
  // The only solutions that I have is to have MIG tool remove the entire internal codes that doesn't belongs to the Configuration..
  //

               // Inputs from Application CMD Port

               if (C_PORT_ENABLE[0] == 1'b1)
               begin

                   assign mig_p0_arb_en      =      p0_arb_en ;
                   assign mig_p0_cmd_clk     =      p0_cmd_clk  ;
                   assign mig_p0_cmd_en      =      p0_cmd_en   ;
                   assign mig_p0_cmd_ra      =      p0_cmd_ra  ;
                   assign mig_p0_cmd_ba      =      p0_cmd_ba   ;
                   assign mig_p0_cmd_ca      =      p0_cmd_ca  ;
                   assign mig_p0_cmd_instr   =      p0_cmd_instr;
                   assign mig_p0_cmd_bl      =      {(p0_cmd_instr[2] | p0_cmd_bl[5]),p0_cmd_bl[4:0]}  ;
                   assign p0_cmd_empty       =      mig_p0_cmd_empty;
                   assign p0_cmd_full        =      mig_p0_cmd_full ;
                   
               end else
               begin
               
                   assign mig_p0_arb_en      =     'b0;
                   assign mig_p0_cmd_clk     =     'b0;
                   assign mig_p0_cmd_en      =     'b0;
                   assign mig_p0_cmd_ra      =     'b0;
                   assign mig_p0_cmd_ba      =     'b0;
                   assign mig_p0_cmd_ca      =     'b0;
                   assign mig_p0_cmd_instr   =     'b0;
                   assign mig_p0_cmd_bl      =     'b0;
                   assign p0_cmd_empty       =     'b0;
                   assign p0_cmd_full        =     'b0;
                   
               end
               

               if (C_PORT_ENABLE[1] == 1'b1)
               begin


                   assign mig_p1_arb_en      =      p1_arb_en ;
                   assign mig_p1_cmd_clk     =      p1_cmd_clk  ;
                   assign mig_p1_cmd_en      =      p1_cmd_en   ;
                   assign mig_p1_cmd_ra      =      p1_cmd_ra  ;
                   assign mig_p1_cmd_ba      =      p1_cmd_ba   ;
                   assign mig_p1_cmd_ca      =      p1_cmd_ca  ;
                   assign mig_p1_cmd_instr   =      p1_cmd_instr;
                   assign mig_p1_cmd_bl      =      {(p1_cmd_instr[2] | p1_cmd_bl[5]),p1_cmd_bl[4:0]}  ;
                   assign p1_cmd_empty       =      mig_p1_cmd_empty;
                   assign p1_cmd_full        =      mig_p1_cmd_full ;
                   
               end else
               begin
                   assign mig_p1_arb_en      =     'b0;
                   assign mig_p1_cmd_clk     =     'b0;
                   assign mig_p1_cmd_en      =     'b0;
                   assign mig_p1_cmd_ra      =     'b0;
                   assign mig_p1_cmd_ba      =     'b0;
                   assign mig_p1_cmd_ca      =     'b0;
                   assign mig_p1_cmd_instr   =     'b0;
                   assign mig_p1_cmd_bl      =     'b0;
                   assign p1_cmd_empty       =      'b0;
                   assign p1_cmd_full        =      'b0;
                   
                   
               end
               

               if (C_PORT_ENABLE[2] == 1'b1)
               begin

                   assign mig_p2_arb_en      =      p2_arb_en ;
                   assign mig_p2_cmd_clk     =      p2_cmd_clk  ;
                   assign mig_p2_cmd_en      =      p2_cmd_en   ;
                   assign mig_p2_cmd_ra      =      p2_cmd_ra  ;
                   assign mig_p2_cmd_ba      =      p2_cmd_ba   ;
                   assign mig_p2_cmd_ca      =      p2_cmd_ca  ;
                   assign mig_p2_cmd_instr   =      p2_cmd_instr;
                   assign mig_p2_cmd_bl      =      {(p2_cmd_instr[2] | p2_cmd_bl[5]),p2_cmd_bl[4:0]}  ;
                   assign p2_cmd_empty   =      mig_p2_cmd_empty;
                   assign p2_cmd_full    =      mig_p2_cmd_full ;
                   
               end else
               begin

                   assign mig_p2_arb_en      =      'b0;
                   assign mig_p2_cmd_clk     =      'b0;
                   assign mig_p2_cmd_en      =      'b0;
                   assign mig_p2_cmd_ra      =      'b0;
                   assign mig_p2_cmd_ba      =      'b0;
                   assign mig_p2_cmd_ca      =      'b0;
                   assign mig_p2_cmd_instr   =      'b0;
                   assign mig_p2_cmd_bl      =      'b0;
                   assign p2_cmd_empty   =       'b0;
                   assign p2_cmd_full    =       'b0;
                   
               end
               
 

               if (C_PORT_ENABLE[3] == 1'b1)
               begin

                   assign mig_p3_arb_en    =        p3_arb_en ;
                   assign mig_p3_cmd_clk     =      p3_cmd_clk  ;
                   assign mig_p3_cmd_en      =      p3_cmd_en   ;
                   assign mig_p3_cmd_ra      =      p3_cmd_ra  ;
                   assign mig_p3_cmd_ba      =      p3_cmd_ba   ;
                   assign mig_p3_cmd_ca      =      p3_cmd_ca  ;
                   assign mig_p3_cmd_instr   =      p3_cmd_instr;
                   assign mig_p3_cmd_bl      =      {(p3_cmd_instr[2] | p3_cmd_bl[5]),p3_cmd_bl[4:0]}  ;
                   assign p3_cmd_empty   =      mig_p3_cmd_empty;
                   assign p3_cmd_full    =      mig_p3_cmd_full ;
                   
               end else
               begin
                   assign mig_p3_arb_en    =       'b0;
                   assign mig_p3_cmd_clk     =     'b0;
                   assign mig_p3_cmd_en      =     'b0;
                   assign mig_p3_cmd_ra      =     'b0;
                   assign mig_p3_cmd_ba      =     'b0;
                   assign mig_p3_cmd_ca      =     'b0;
                   assign mig_p3_cmd_instr   =     'b0;
                   assign mig_p3_cmd_bl      =     'b0;
                   assign p3_cmd_empty   =     'b0;
                   assign p3_cmd_full    =     'b0;
                   
               end
               
               if (C_PORT_ENABLE[4] == 1'b1)
               begin

                   assign mig_p4_arb_en    =        p4_arb_en ;
                   assign mig_p4_cmd_clk     =      p4_cmd_clk  ;
                   assign mig_p4_cmd_en      =      p4_cmd_en   ;
                   assign mig_p4_cmd_ra      =      p4_cmd_ra  ;
                   assign mig_p4_cmd_ba      =      p4_cmd_ba   ;
                   assign mig_p4_cmd_ca      =      p4_cmd_ca  ;
                   assign mig_p4_cmd_instr   =      p4_cmd_instr;
                   assign mig_p4_cmd_bl      =      {(p4_cmd_instr[2] | p4_cmd_bl[5]),p4_cmd_bl[4:0]}  ;
                   assign p4_cmd_empty   =      mig_p4_cmd_empty;
                   assign p4_cmd_full    =      mig_p4_cmd_full ;
                   
               end else
               begin
                   assign mig_p4_arb_en      =      'b0;
                   assign mig_p4_cmd_clk     =      'b0;
                   assign mig_p4_cmd_en      =      'b0;
                   assign mig_p4_cmd_ra      =      'b0;
                   assign mig_p4_cmd_ba      =      'b0;
                   assign mig_p4_cmd_ca      =      'b0;
                   assign mig_p4_cmd_instr   =      'b0;
                   assign mig_p4_cmd_bl      =      'b0;
                   assign p4_cmd_empty   =      'b0;
                   assign p4_cmd_full    =      'b0;
                   


               end

               if (C_PORT_ENABLE[5] == 1'b1)
               begin

                   assign  mig_p5_arb_en    =     p5_arb_en ;
                   assign  mig_p5_cmd_clk   =     p5_cmd_clk  ;
                   assign  mig_p5_cmd_en    =     p5_cmd_en   ;
                   assign  mig_p5_cmd_ra    =     p5_cmd_ra  ;
                   assign  mig_p5_cmd_ba    =     p5_cmd_ba   ;
                   assign  mig_p5_cmd_ca    =     p5_cmd_ca  ;
                   assign mig_p5_cmd_instr  =     p5_cmd_instr;
                   assign mig_p5_cmd_bl     =     {(p5_cmd_instr[2] | p5_cmd_bl[5]),p5_cmd_bl[4:0]}  ;
                   assign p5_cmd_empty   =     mig_p5_cmd_empty;
                   assign p5_cmd_full    =     mig_p5_cmd_full ;
                   
               end else
               begin
                   assign  mig_p5_arb_en     =   'b0;
                   assign  mig_p5_cmd_clk    =   'b0;
                   assign  mig_p5_cmd_en     =   'b0;
                   assign  mig_p5_cmd_ra     =   'b0;
                   assign  mig_p5_cmd_ba     =   'b0;
                   assign  mig_p5_cmd_ca     =   'b0;
                   assign mig_p5_cmd_instr   =   'b0;
                   assign mig_p5_cmd_bl      =   'b0;
                   assign p5_cmd_empty   =     'b0;
                   assign p5_cmd_full    =     'b0;
                   
               
               end




              // Inputs from Application User Port
              
              // Port 0
               if (C_PORT_ENABLE[0] == 1'b1)
               begin
                assign mig_p0_wr_clk   = p0_wr_clk;
                assign mig_p0_rd_clk   = p0_rd_clk;
                assign mig_p0_wr_en    = p0_wr_en;
                assign mig_p0_rd_en    = p0_rd_en;
                assign mig_p0_wr_mask  = p0_wr_mask[3:0];
                assign mig_p0_wr_data  = p0_wr_data[31:0];
                assign p0_rd_data        = mig_p0_rd_data;
                assign p0_rd_full        = mig_p0_rd_full;
                assign p0_rd_empty       = mig_p0_rd_empty;
                assign p0_rd_error       = mig_p0_rd_error;
                assign p0_wr_error       = mig_p0_wr_error;
                assign p0_rd_overflow    = mig_p0_rd_overflow;
                assign p0_wr_underrun    = mig_p0_wr_underrun;
                assign p0_wr_empty       = mig_p0_wr_empty;
                assign p0_wr_full        = mig_p0_wr_full;
                assign p0_wr_count       = mig_p0_wr_count;
                assign p0_rd_count       = mig_p0_rd_count  ; 
                
                
               end
               else
               begin
                assign mig_p0_wr_clk     = 'b0;
                assign mig_p0_rd_clk     = 'b0;
                assign mig_p0_wr_en      = 'b0;
                assign mig_p0_rd_en      = 'b0;
                assign mig_p0_wr_mask    = 'b0;
                assign mig_p0_wr_data    = 'b0;
                assign p0_rd_data        = 'b0;
                assign p0_rd_full        = 'b0;
                assign p0_rd_empty       = 'b0;
                assign p0_rd_error       = 'b0;
                assign p0_wr_error       = 'b0;
                assign p0_rd_overflow    = 'b0;
                assign p0_wr_underrun    = 'b0;
                assign p0_wr_empty       = 'b0;
                assign p0_wr_full        = 'b0;
                assign p0_wr_count       = 'b0;
                assign p0_rd_count       = 'b0;
                
                
               end
              
              
              // Port 1
               if (C_PORT_ENABLE[1] == 1'b1)
               begin
              
                assign mig_p1_wr_clk   = p1_wr_clk;
                assign mig_p1_rd_clk   = p1_rd_clk;                
                assign mig_p1_wr_en    = p1_wr_en;
                assign mig_p1_wr_mask  = p1_wr_mask[3:0];                
                assign mig_p1_wr_data  = p1_wr_data[31:0];
                assign mig_p1_rd_en    = p1_rd_en;
                assign p1_rd_data     = mig_p1_rd_data;
                assign p1_rd_empty    = mig_p1_rd_empty;
                assign p1_rd_full     = mig_p1_rd_full;
                assign p1_rd_error    = mig_p1_rd_error;
                assign p1_wr_error    = mig_p1_wr_error;
                assign p1_rd_overflow = mig_p1_rd_overflow;
                assign p1_wr_underrun    = mig_p1_wr_underrun;
                assign p1_wr_empty    = mig_p1_wr_empty;
                assign p1_wr_full    = mig_p1_wr_full;
                assign p1_wr_count  = mig_p1_wr_count;
                assign p1_rd_count  = mig_p1_rd_count  ; 
                
               end else
               begin
              
                assign mig_p1_wr_clk   = 'b0;
                assign mig_p1_rd_clk   = 'b0;            
                assign mig_p1_wr_en    = 'b0;
                assign mig_p1_wr_mask  = 'b0;          
                assign mig_p1_wr_data  = 'b0;
                assign mig_p1_rd_en    = 'b0;
                assign p1_rd_data     =  'b0;
                assign p1_rd_empty    =  'b0;
                assign p1_rd_full     =  'b0;
                assign p1_rd_error    =  'b0;
                assign p1_wr_error    =  'b0;
                assign p1_rd_overflow =  'b0;
                assign p1_wr_underrun =  'b0;
                assign p1_wr_empty    =  'b0;
                assign p1_wr_full     =  'b0;
                assign p1_wr_count    =  'b0;
                assign p1_rd_count    =  'b0;
                
                
               end
                
                
                


// whenever PORT 2 is in Write mode           
         if(C_PORT_CONFIG[183:160] == "B32" && C_PORT_CONFIG[119:96] == "W32") begin : u_config1_2W
                  if (C_PORT_ENABLE[2] == 1'b1)
                  begin
                       assign mig_p2_clk      = p2_wr_clk;
                       assign mig_p2_wr_data  = p2_wr_data[31:0];
                       assign mig_p2_wr_mask  = p2_wr_mask[3:0];
                       assign mig_p2_en       = p2_wr_en; // this signal will not shown up if the port 5 is for read dir
                       assign p2_wr_error     = mig_p2_error;                       
                       assign p2_wr_full      = mig_p2_full;
                       assign p2_wr_empty     = mig_p2_empty;
                       assign p2_wr_underrun  = mig_p2_underrun;
                       assign p2_wr_count     = mig_p2_count  ; // wr port
                       
                       
                  end else
                  begin
                       assign mig_p2_clk      = 'b0;
                       assign mig_p2_wr_data  = 'b0;
                       assign mig_p2_wr_mask  = 'b0;
                       assign mig_p2_en       = 'b0;
                       assign p2_wr_error     = 'b0;
                       assign p2_wr_full      = 'b0;
                       assign p2_wr_empty     = 'b0;
                       assign p2_wr_underrun  = 'b0;
                       assign p2_wr_count     = 'b0;
                                                
                  end                           
                   assign p2_rd_data        = 'b0;
                   assign p2_rd_overflow    = 'b0;
                   assign p2_rd_error       = 'b0;
                   assign p2_rd_full        = 'b0;
                   assign p2_rd_empty       = 'b0;
                   assign p2_rd_count       = 'b0;
//                   assign p2_rd_error       = 'b0;
                       
                       
                         
         end else if(C_PORT_CONFIG[183:160] == "B32" && C_PORT_CONFIG[119:96] == "R32") begin : u_config1_2R

                  if (C_PORT_ENABLE[2] == 1'b1)
                  begin
                       assign mig_p2_clk        = p2_rd_clk;
                       assign p2_rd_data        = mig_p2_rd_data;
                       assign mig_p2_en         = p2_rd_en;  
                       assign p2_rd_overflow    = mig_p2_overflow;
                       assign p2_rd_error       = mig_p2_error;
                       assign p2_rd_full        = mig_p2_full;
                       assign p2_rd_empty       = mig_p2_empty;
                       assign p2_rd_count       = mig_p2_count  ; // wr port
                       
                  end else       
                  begin
                       assign mig_p2_clk        = 'b0;
                       assign p2_rd_data        = 'b0;
                       assign mig_p2_en         = 'b0;
                       
                       assign p2_rd_overflow    = 'b0;
                       assign p2_rd_error       = 'b0;
                       assign p2_rd_full        = 'b0;
                       assign p2_rd_empty       = 'b0;
                       assign p2_rd_count       = 'b0;
                       
                  end
                  assign mig_p2_wr_data  = 'b0;
                  assign mig_p2_wr_mask  = 'b0;
                  assign p2_wr_error     = 'b0;
                  assign p2_wr_full      = 'b0;
                  assign p2_wr_empty     = 'b0;
                  assign p2_wr_underrun  = 'b0;
                  assign p2_wr_count     = 'b0;
          
          end 
          if(C_PORT_CONFIG[183:160] == "B32" && C_PORT_CONFIG[87:64]  == "W32") begin : u_config1_3W
// whenever PORT 3 is in Write mode         

                  if (C_PORT_ENABLE[3] == 1'b1)
                  begin

                       assign mig_p3_clk   = p3_wr_clk;
                       assign mig_p3_wr_data  = p3_wr_data[31:0];
                       assign mig_p3_wr_mask  = p3_wr_mask[3:0];
                       assign mig_p3_en       = p3_wr_en; 
                       assign p3_wr_full      = mig_p3_full;
                       assign p3_wr_empty     = mig_p3_empty;
                       assign p3_wr_underrun  = mig_p3_underrun;
                       assign p3_wr_count     = mig_p3_count  ; // wr port
                       assign p3_wr_error     = mig_p3_error;
                       
                  end else 
                  begin
                       assign mig_p3_clk      = 'b0;
                       assign mig_p3_wr_data  = 'b0;
                       assign mig_p3_wr_mask  = 'b0;
                       assign mig_p3_en       = 'b0;
                       assign p3_wr_full      = 'b0;
                       assign p3_wr_empty     = 'b0;
                       assign p3_wr_underrun  = 'b0;
                       assign p3_wr_count     = 'b0;
                       assign p3_wr_error     = 'b0;
                                                
                  end
                   assign p3_rd_overflow = 'b0;
                   assign p3_rd_error    = 'b0;
                   assign p3_rd_full     = 'b0;
                   assign p3_rd_empty    = 'b0;
                   assign p3_rd_count    = 'b0;
                   assign p3_rd_data     = 'b0;
       
                       
         end else if(C_PORT_CONFIG[183:160] == "B32" && C_PORT_CONFIG[87:64]  == "R32") begin : u_config1_3R
       
                  if (C_PORT_ENABLE[3] == 1'b1)
                  begin

                       assign mig_p3_clk     = p3_rd_clk;
                       assign p3_rd_data     = mig_p3_rd_data;                
                       assign mig_p3_en      = p3_rd_en;  // this signal will not shown up if the port 5 is for write dir
                       assign p3_rd_overflow = mig_p3_overflow;
                       assign p3_rd_error    = mig_p3_error;
                       assign p3_rd_full     = mig_p3_full;
                       assign p3_rd_empty    = mig_p3_empty;
                       assign p3_rd_count    = mig_p3_count  ; // wr port
                  end else
                  begin 
                       assign mig_p3_clk     = 'b0;
                       assign mig_p3_en      = 'b0;
                       assign p3_rd_overflow = 'b0;
                       assign p3_rd_full     = 'b0;
                       assign p3_rd_empty    = 'b0;
                       assign p3_rd_count    = 'b0;
                       assign p3_rd_error    = 'b0;
                       assign p3_rd_data     = 'b0;
                  end                  
                  assign p3_wr_full      = 'b0;
                  assign p3_wr_empty     = 'b0;
                  assign p3_wr_underrun  = 'b0;
                  assign p3_wr_count     = 'b0;
                  assign p3_wr_error     = 'b0;
                  assign mig_p3_wr_data  = 'b0;
                  assign mig_p3_wr_mask  = 'b0;
         end 
         if(C_PORT_CONFIG[183:160] == "B32" && C_PORT_CONFIG[55:32]  == "W32") begin : u_config1_4W
       // whenever PORT 4 is in Write mode       

                  if (C_PORT_ENABLE[4] == 1'b1)
                  begin
       
                       assign mig_p4_clk      = p4_wr_clk;
                       assign mig_p4_wr_data  = p4_wr_data[31:0];
                       assign mig_p4_wr_mask  = p4_wr_mask[3:0];
                       assign mig_p4_en       = p4_wr_en; // this signal will not shown up if the port 5 is for read dir
                       assign p4_wr_full      = mig_p4_full;
                       assign p4_wr_empty     = mig_p4_empty;
                       assign p4_wr_underrun  = mig_p4_underrun;
                       assign p4_wr_count     = mig_p4_count  ; // wr port
                       assign p4_wr_error     = mig_p4_error;

                  end else
                  begin
                       assign mig_p4_clk      = 'b0;
                       assign mig_p4_wr_data  = 'b0;
                       assign mig_p4_wr_mask  = 'b0;
                       assign mig_p4_en       = 'b0;
                       assign p4_wr_full      = 'b0;
                       assign p4_wr_empty     = 'b0;
                       assign p4_wr_underrun  = 'b0;
                       assign p4_wr_count     = 'b0;
                       assign p4_wr_error     = 'b0;
                  end                           
                   assign p4_rd_overflow    = 'b0;
                   assign p4_rd_error       = 'b0;
                   assign p4_rd_full        = 'b0;
                   assign p4_rd_empty       = 'b0;
                   assign p4_rd_count       = 'b0;
                   assign p4_rd_data        = 'b0;
       
         end else if(C_PORT_CONFIG[183:160] == "B32" && C_PORT_CONFIG[55:32]  == "R32") begin : u_config1_4R
                       
                  if (C_PORT_ENABLE[4] == 1'b1)
                  begin
                       assign mig_p4_clk        = p4_rd_clk;
                       assign p4_rd_data        = mig_p4_rd_data;                
                       assign mig_p4_en         = p4_rd_en;  // this signal will not shown up if the port 5 is for write dir
                       assign p4_rd_overflow    = mig_p4_overflow;
                       assign p4_rd_error       = mig_p4_error;
                       assign p4_rd_full        = mig_p4_full;
                       assign p4_rd_empty       = mig_p4_empty;
                       assign p4_rd_count       = mig_p4_count  ; // wr port
                       
                  end else
                  begin
                       assign mig_p4_clk        = 'b0;
                       assign p4_rd_data        = 'b0;
                       assign mig_p4_en         = 'b0;
                       assign p4_rd_overflow    = 'b0;
                       assign p4_rd_error       = 'b0;
                       assign p4_rd_full        = 'b0;
                       assign p4_rd_empty       = 'b0;
                       assign p4_rd_count       = 'b0;
                  end                  
                  assign p4_wr_full      = 'b0;
                  assign p4_wr_empty     = 'b0;
                  assign p4_wr_underrun  = 'b0;
                  assign p4_wr_count     = 'b0;
                  assign p4_wr_error     = 'b0;
                  assign mig_p4_wr_data  = 'b0;
                  assign mig_p4_wr_mask  = 'b0;


                       
                       
         end 
         
         if(C_PORT_CONFIG[183:160] == "B32" && C_PORT_CONFIG[23:0] == "W32") begin : u_config1_5W
       // whenever PORT 5 is in Write mode           

                       
                  if (C_PORT_ENABLE[5] == 1'b1)
                  begin
                       assign mig_p5_clk   = p5_wr_clk;
                       assign mig_p5_wr_data  = p5_wr_data[31:0];
                       assign mig_p5_wr_mask  = p5_wr_mask[3:0];
                       assign mig_p5_en       = p5_wr_en; 
                       assign p5_wr_full      = mig_p5_full;
                       assign p5_wr_empty     = mig_p5_empty;
                       assign p5_wr_underrun  = mig_p5_underrun;
                       assign p5_wr_count     = mig_p5_count  ; 
                       assign p5_wr_error     = mig_p5_error;
                       
                  end else
                  begin
                       assign mig_p5_clk      = 'b0;
                       assign mig_p5_wr_data  = 'b0;
                       assign mig_p5_wr_mask  = 'b0;
                       assign mig_p5_en       = 'b0;
                       assign p5_wr_full      = 'b0;
                       assign p5_wr_empty     = 'b0;
                       assign p5_wr_underrun  = 'b0;
                       assign p5_wr_count     = 'b0;
                       assign p5_wr_error     = 'b0;
                  end                           
                   assign p5_rd_data        = 'b0;
                   assign p5_rd_overflow    = 'b0;
                   assign p5_rd_error       = 'b0;
                   assign p5_rd_full        = 'b0;
                   assign p5_rd_empty       = 'b0;
                   assign p5_rd_count       = 'b0;
                  
       
                       
                         
         end else if(C_PORT_CONFIG[183:160] == "B32" && C_PORT_CONFIG[23:0] == "R32") begin : u_config1_5R

                  if (C_PORT_ENABLE[5] == 1'b1)
                  begin

                       assign mig_p5_clk        = p5_rd_clk;
                       assign p5_rd_data        = mig_p5_rd_data;                
                       assign mig_p5_en         = p5_rd_en;  
                       assign p5_rd_overflow    = mig_p5_overflow;
                       assign p5_rd_error       = mig_p5_error;
                       assign p5_rd_full        = mig_p5_full;
                       assign p5_rd_empty       = mig_p5_empty;
                       assign p5_rd_count       = mig_p5_count  ; 
                       
                 end else
                 begin
                       assign mig_p5_clk        = 'b0;
                       assign p5_rd_data        = 'b0;           
                       assign mig_p5_en         = 'b0;
                       assign p5_rd_overflow    = 'b0;
                       assign p5_rd_error       = 'b0;
                       assign p5_rd_full        = 'b0;
                       assign p5_rd_empty       = 'b0;
                       assign p5_rd_count       = 'b0;
                 
                 end
                 assign p5_wr_full      = 'b0;
                 assign p5_wr_empty     = 'b0;
                 assign p5_wr_underrun  = 'b0;
                 assign p5_wr_count     = 'b0;
                 assign p5_wr_error     = 'b0;
                 assign mig_p5_wr_data  = 'b0;
                 assign mig_p5_wr_mask  = 'b0;
                       
         end
                
  end else if(C_PORT_CONFIG == "B32_B32_B32_B32" ) begin : u_config_2

           
               // Inputs from Application CMD Port
               // *************  need to hook up rd /wr error outputs
               
                  if (C_PORT_ENABLE[0] == 1'b1)
                  begin
                           // command port signals
                           assign mig_p0_arb_en      =      p0_arb_en ;
                           assign mig_p0_cmd_clk     =      p0_cmd_clk  ;
                           assign mig_p0_cmd_en      =      p0_cmd_en   ;
                           assign mig_p0_cmd_ra      =      p0_cmd_ra  ;
                           assign mig_p0_cmd_ba      =      p0_cmd_ba   ;
                           assign mig_p0_cmd_ca      =      p0_cmd_ca  ;
                           assign mig_p0_cmd_instr   =      p0_cmd_instr;
                           assign mig_p0_cmd_bl      =       {(p0_cmd_instr[2] | p0_cmd_bl[5]),p0_cmd_bl[4:0]}   ;
                           
                           // Data port signals
                           assign mig_p0_rd_en    = p0_rd_en;                            
                           assign mig_p0_wr_clk   = p0_wr_clk;
                           assign mig_p0_rd_clk   = p0_rd_clk;
                           assign mig_p0_wr_en    = p0_wr_en;
                           assign mig_p0_wr_data  = p0_wr_data[31:0]; 
                           assign mig_p0_wr_mask  = p0_wr_mask[3:0];
                           assign p0_wr_count     = mig_p0_wr_count;
                           assign p0_rd_count  = mig_p0_rd_count  ; 

                           
                           
                 end else
                 begin
                           assign mig_p0_arb_en      =       'b0;
                           assign mig_p0_cmd_clk     =       'b0;
                           assign mig_p0_cmd_en      =       'b0;
                           assign mig_p0_cmd_ra      =       'b0;
                           assign mig_p0_cmd_ba      =       'b0;
                           assign mig_p0_cmd_ca      =       'b0;
                           assign mig_p0_cmd_instr   =       'b0;
                           assign mig_p0_cmd_bl      =       'b0;
                           
                           assign mig_p0_rd_en    = 'b0;                    
                           assign mig_p0_wr_clk   = 'b0;
                           assign mig_p0_rd_clk   = 'b0;
                           assign mig_p0_wr_en    = 'b0;
                           assign mig_p0_wr_data  = 'b0; 
                           assign mig_p0_wr_mask  = 'b0;
                           assign p0_wr_count     = 'b0;
                           assign p0_rd_count     = 'b0;

                           
                 end                           
                           
                           assign p0_cmd_empty       =      mig_p0_cmd_empty ;
                           assign p0_cmd_full        =      mig_p0_cmd_full  ;
                           
                           
                  if (C_PORT_ENABLE[1] == 1'b1)
                  begin
                           // command port signals

                           assign mig_p1_arb_en      =      p1_arb_en ;
                           assign mig_p1_cmd_clk     =      p1_cmd_clk  ;
                           assign mig_p1_cmd_en      =      p1_cmd_en   ;
                           assign mig_p1_cmd_ra      =      p1_cmd_ra  ;
                           assign mig_p1_cmd_ba      =      p1_cmd_ba   ;
                           assign mig_p1_cmd_ca      =      p1_cmd_ca  ;
                           assign mig_p1_cmd_instr   =      p1_cmd_instr;
                           assign mig_p1_cmd_bl      =      {(p1_cmd_instr[2] | p1_cmd_bl[5]),p1_cmd_bl[4:0]}  ;
                           // Data port signals
                 
                            assign mig_p1_wr_en    = p1_wr_en;
                            assign mig_p1_wr_clk   = p1_wr_clk;
                            assign mig_p1_rd_en    = p1_rd_en;
                            assign mig_p1_wr_data  = p1_wr_data[31:0];
                            assign mig_p1_wr_mask  = p1_wr_mask[3:0];                
                            assign mig_p1_rd_clk   = p1_rd_clk;
                            assign p1_wr_count     = mig_p1_wr_count;
                            assign p1_rd_count     = mig_p1_rd_count;
                           
                  end else
                  begin

                           assign mig_p1_arb_en      =       'b0;
                           assign mig_p1_cmd_clk     =       'b0;
                           assign mig_p1_cmd_en      =       'b0;
                           assign mig_p1_cmd_ra      =       'b0;
                           assign mig_p1_cmd_ba      =       'b0;
                           assign mig_p1_cmd_ca      =       'b0;
                           assign mig_p1_cmd_instr   =       'b0;
                           assign mig_p1_cmd_bl      =       'b0;
                           // Data port signals
                           assign mig_p1_wr_en    = 'b0; 
                           assign mig_p1_wr_clk   = 'b0;
                           assign mig_p1_rd_en    = 'b0;
                           assign mig_p1_wr_data  = 'b0;
                           assign mig_p1_wr_mask  = 'b0;                
                           assign mig_p1_rd_clk   = 'b0;
                            assign p1_wr_count     = 'b0;
                            assign p1_rd_count     = 'b0;
                  
                  end
                           
                           
                           assign p1_cmd_empty       =      mig_p1_cmd_empty ;
                           assign p1_cmd_full        =      mig_p1_cmd_full  ;
 
                  if (C_PORT_ENABLE[2] == 1'b1)
                  begin   //MCB Physical port               Logical Port
                           assign mig_p2_arb_en      =      p2_arb_en ;
                           assign mig_p2_cmd_clk     =      p2_cmd_clk  ;
                           assign mig_p2_cmd_en      =      p2_cmd_en   ;
                           assign mig_p2_cmd_ra      =      p2_cmd_ra  ;
                           assign mig_p2_cmd_ba      =      p2_cmd_ba   ;
                           assign mig_p2_cmd_ca      =      p2_cmd_ca  ;
                           assign mig_p2_cmd_instr   =      p2_cmd_instr;
                           assign mig_p2_cmd_bl      =      {(p2_cmd_instr[2] | p2_cmd_bl[5]),p2_cmd_bl[4:0]}   ;
                           
                            assign mig_p2_en       = p2_rd_en;
                            assign mig_p2_clk      = p2_rd_clk;
                            assign mig_p3_en       = p2_wr_en;
                            assign mig_p3_clk      = p2_wr_clk;
                            assign mig_p3_wr_data  = p2_wr_data[31:0];
                            assign mig_p3_wr_mask  = p2_wr_mask[3:0];
                            assign p2_wr_count     = mig_p3_count;
                            assign p2_rd_count     = mig_p2_count;
                           
                  end else
                  begin

                           assign mig_p2_arb_en      =      'b0;
                           assign mig_p2_cmd_clk     =      'b0;
                           assign mig_p2_cmd_en      =      'b0;
                           assign mig_p2_cmd_ra      =      'b0;
                           assign mig_p2_cmd_ba      =      'b0;
                           assign mig_p2_cmd_ca      =      'b0;
                           assign mig_p2_cmd_instr   =      'b0;
                           assign mig_p2_cmd_bl      =      'b0;

                            assign mig_p2_en       = 'b0; 
                            assign mig_p2_clk      = 'b0;
                            assign mig_p3_en       = 'b0;
                            assign mig_p3_clk      = 'b0;
                            assign mig_p3_wr_data  = 'b0; 
                            assign mig_p3_wr_mask  = 'b0;
                            assign p2_rd_count     = 'b0;
                            assign p2_wr_count     = 'b0;
                           
                 end                           
                         
                           assign p2_cmd_empty       =      mig_p2_cmd_empty ;
                           assign p2_cmd_full        =      mig_p2_cmd_full  ;
  
                 if (C_PORT_ENABLE[3] == 1'b1)
                  begin   //MCB Physical port               Logical Port
                           assign mig_p4_arb_en      =      p3_arb_en ;
                           assign mig_p4_cmd_clk     =      p3_cmd_clk  ;
                           assign mig_p4_cmd_en      =      p3_cmd_en   ;
                           assign mig_p4_cmd_ra      =      p3_cmd_ra  ;
                           assign mig_p4_cmd_ba      =      p3_cmd_ba   ;
                           assign mig_p4_cmd_ca      =      p3_cmd_ca  ;
                           assign mig_p4_cmd_instr   =      p3_cmd_instr;
                           assign mig_p4_cmd_bl      =      {(p3_cmd_instr[2] | p3_cmd_bl[5]),p3_cmd_bl[4:0]}  ;

                           assign mig_p4_clk      = p3_rd_clk;
                           assign mig_p4_en       = p3_rd_en;                            
                           assign mig_p5_clk      = p3_wr_clk;
                           assign mig_p5_en       = p3_wr_en; 
                           assign mig_p5_wr_data  = p3_wr_data[31:0];
                           assign mig_p5_wr_mask  = p3_wr_mask[3:0];
                           assign p3_rd_count     = mig_p4_count;
                           assign p3_wr_count     = mig_p5_count;
                           
                           
                  end else
                  begin
                           assign mig_p4_arb_en      =     'b0;
                           assign mig_p4_cmd_clk     =     'b0;
                           assign mig_p4_cmd_en      =     'b0;
                           assign mig_p4_cmd_ra      =     'b0;
                           assign mig_p4_cmd_ba      =     'b0;
                           assign mig_p4_cmd_ca      =     'b0;
                           assign mig_p4_cmd_instr   =     'b0;
                           assign mig_p4_cmd_bl      =     'b0;
                           
                            assign mig_p4_clk      = 'b0; 
                            assign mig_p4_en       = 'b0;                   
                            assign mig_p5_clk      = 'b0;
                            assign mig_p5_en       = 'b0;
                            assign mig_p5_wr_data  = 'b0; 
                            assign mig_p5_wr_mask  = 'b0;
                            assign p3_rd_count     = 'b0;
                            assign p3_wr_count     = 'b0;
                           
                          
                           
                  end         
                           
                           assign p3_cmd_empty       =      mig_p4_cmd_empty ;
                           assign p3_cmd_full        =      mig_p4_cmd_full  ;
                           
                           
                            // outputs to Applications User Port
                            assign p0_rd_data     = mig_p0_rd_data;
                            assign p1_rd_data     = mig_p1_rd_data;
                            assign p2_rd_data     = mig_p2_rd_data;
                            assign p3_rd_data     = mig_p4_rd_data;

                            assign p0_rd_empty    = mig_p0_rd_empty;
                            assign p1_rd_empty    = mig_p1_rd_empty;
                            assign p2_rd_empty    = mig_p2_empty;
                            assign p3_rd_empty    = mig_p4_empty;

                            assign p0_rd_full     = mig_p0_rd_full;
                            assign p1_rd_full     = mig_p1_rd_full;
                            assign p2_rd_full     = mig_p2_full;
                            assign p3_rd_full     = mig_p4_full;

                            assign p0_rd_error    = mig_p0_rd_error;
                            assign p1_rd_error    = mig_p1_rd_error;
                            assign p2_rd_error    = mig_p2_error;
                            assign p3_rd_error    = mig_p4_error;
                            
                            assign p0_rd_overflow = mig_p0_rd_overflow;
                            assign p1_rd_overflow = mig_p1_rd_overflow;
                            assign p2_rd_overflow = mig_p2_overflow;
                            assign p3_rd_overflow = mig_p4_overflow;

                            assign p0_wr_underrun = mig_p0_wr_underrun;
                            assign p1_wr_underrun = mig_p1_wr_underrun;
                            assign p2_wr_underrun = mig_p3_underrun;
                            assign p3_wr_underrun = mig_p5_underrun;
                            
                            assign p0_wr_empty    = mig_p0_wr_empty;
                            assign p1_wr_empty    = mig_p1_wr_empty;
                            assign p2_wr_empty    = mig_p3_empty; 
                            assign p3_wr_empty    = mig_p5_empty; 
 
                            assign p0_wr_full    = mig_p0_wr_full;
                            assign p1_wr_full    = mig_p1_wr_full;
                            assign p2_wr_full    = mig_p3_full;
                            assign p3_wr_full    = mig_p5_full;

                            assign p0_wr_error    = mig_p0_wr_error;
                            assign p1_wr_error    = mig_p1_wr_error;
                            assign p2_wr_error    = mig_p3_error;
                            assign p3_wr_error    = mig_p5_error;

     // unused ports signals
                           assign p4_cmd_empty        =     1'b0;
                           assign p4_cmd_full         =     1'b0;
                           assign mig_p2_wr_mask  = 'b0;
                           assign mig_p4_wr_mask  = 'b0;

                           assign mig_p2_wr_data     = 'b0;
                           assign mig_p4_wr_data     = 'b0;

                           assign p5_cmd_empty        =     1'b0;
                           assign p5_cmd_full         =     1'b0;
     
 
                            assign mig_p3_cmd_clk     =      1'b0;
                            assign mig_p3_cmd_en      =      1'b0;
                            assign mig_p3_cmd_ra      =      15'd0;
                            assign mig_p3_cmd_ba      =      3'd0;
                            assign mig_p3_cmd_ca      =      12'd0;
                            assign mig_p3_cmd_instr   =      3'd0;
                            assign mig_p3_cmd_bl      =      6'd0;
                            assign mig_p3_arb_en      =      1'b0;  // physical cmd port 3 is not used in this config
                            
                            
                            
                            
                            assign mig_p5_arb_en      =      1'b0;  // physical cmd port 3 is not used in this config
                            assign mig_p5_cmd_clk     =      1'b0;
                            assign mig_p5_cmd_en      =      1'b0;
                            assign mig_p5_cmd_ra      =      15'd0;
                            assign mig_p5_cmd_ba      =      3'd0;
                            assign mig_p5_cmd_ca      =      12'd0;
                            assign mig_p5_cmd_instr   =      3'd0;
                            assign mig_p5_cmd_bl      =      6'd0;



      ////////////////////////////////////////////////////////////////////////////
      /////////////////////////////////////////////////////////////////////////////
      ////     
      ////                         B64_B32_B32
      ////     
      /////////////////////////////////////////////////////////////////////////////
      ////////////////////////////////////////////////////////////////////////////

     
     
  end else if(C_PORT_CONFIG == "B64_B32_B32" ) begin : u_config_3

               // Inputs from Application CMD Port
 
 
       if (C_PORT_ENABLE[0] == 1'b1)
       begin
               assign mig_p0_arb_en      =  p0_arb_en ;
               assign mig_p0_cmd_clk     =  p0_cmd_clk  ;
               assign mig_p0_cmd_en      =  p0_cmd_en   ;
               assign mig_p0_cmd_ra      =  p0_cmd_ra  ;
               assign mig_p0_cmd_ba      =  p0_cmd_ba   ;
               assign mig_p0_cmd_ca      =  p0_cmd_ca  ;
               assign mig_p0_cmd_instr   =  p0_cmd_instr;
               assign mig_p0_cmd_bl      =   {(p0_cmd_instr[2] | p0_cmd_bl[5]),p0_cmd_bl[4:0]}   ;
               assign p0_cmd_empty       =  mig_p0_cmd_empty ;
               assign p0_cmd_full        =  mig_p0_cmd_full  ;

               assign mig_p0_wr_clk   = p0_wr_clk;
               assign mig_p0_rd_clk   = p0_rd_clk;
               assign mig_p1_wr_clk   = p0_wr_clk;
               assign mig_p1_rd_clk   = p0_rd_clk;
                
               if (C_USR_INTERFACE_MODE == "AXI")
                   assign mig_p0_wr_en    = p0_wr_en ;
               else
                   assign mig_p0_wr_en    = p0_wr_en & !p0_wr_full;

               if (C_USR_INTERFACE_MODE == "AXI")
                   assign mig_p1_wr_en    = p0_wr_en ;
               else
                   assign mig_p1_wr_en    = p0_wr_en & !p0_wr_full;
                   
               assign mig_p0_wr_data  = p0_wr_data[31:0];
               assign mig_p0_wr_mask  = p0_wr_mask[3:0];
               assign mig_p1_wr_data  = p0_wr_data[63 : 32];
               assign mig_p1_wr_mask  = p0_wr_mask[7 : 4];     

               assign p0_rd_empty       = mig_p1_rd_empty;
               assign p0_rd_data        = {mig_p1_rd_data , mig_p0_rd_data}; 
               if (C_USR_INTERFACE_MODE == "AXI")
                   assign mig_p0_rd_en    = p0_rd_en ;
               else
                   assign mig_p0_rd_en    = p0_rd_en & !p0_rd_empty;

               if (C_USR_INTERFACE_MODE == "AXI")               
                   assign mig_p1_rd_en    = p0_rd_en ;
                else
                   assign mig_p1_rd_en    = p0_rd_en & !p0_rd_empty;

                assign p0_wr_count       = mig_p1_wr_count;  // B64 for port 0, map most significant port to output
                assign p0_rd_count       = mig_p1_rd_count;
                assign p0_wr_empty       = mig_p1_wr_empty;
                assign p0_wr_error       = mig_p1_wr_error | mig_p0_wr_error;  
                assign p0_wr_full        = mig_p1_wr_full;
                assign p0_wr_underrun    = mig_p1_wr_underrun | mig_p0_wr_underrun; 
                assign p0_rd_overflow    = mig_p1_rd_overflow | mig_p0_rd_overflow; 
                assign p0_rd_error       = mig_p1_rd_error | mig_p0_rd_error; 
                assign p0_rd_full        = mig_p1_rd_full;

 
       end else
       begin
       
               assign mig_p0_arb_en      = 'b0;
               assign mig_p0_cmd_clk     = 'b0;
               assign mig_p0_cmd_en      = 'b0;
               assign mig_p0_cmd_ra      = 'b0;
               assign mig_p0_cmd_ba      = 'b0;
               assign mig_p0_cmd_ca      = 'b0;
               assign mig_p0_cmd_instr   = 'b0;
               assign mig_p0_cmd_bl      = 'b0;
               assign p0_cmd_empty       =  'b0;
               assign p0_cmd_full        =  'b0;


               assign mig_p0_wr_clk   = 'b0;
               assign mig_p0_rd_clk   = 'b0;
               assign mig_p1_wr_clk   = 'b0;
               assign mig_p1_rd_clk   = 'b0;
               
               assign mig_p0_wr_en    = 'b0;
               assign mig_p1_wr_en    = 'b0;
               assign mig_p0_wr_data  = 'b0;
               assign mig_p0_wr_mask  = 'b0;
               assign mig_p1_wr_data  = 'b0;
               assign mig_p1_wr_mask  = 'b0; 

               assign p0_rd_empty       = 'b0;
               assign p0_rd_data        = 'b0;
               assign mig_p0_rd_en      = 'b0;
               assign mig_p1_rd_en      = 'b0;
 
 
               assign p0_wr_count       =  'b0;
               assign p0_rd_count       =  'b0;
               assign p0_wr_empty       =  'b0;
               assign p0_wr_error       =  'b0;
               assign p0_wr_full        =  'b0;
               assign p0_wr_underrun    =  'b0;
               assign p0_rd_overflow    =  'b0;
               assign p0_rd_error       =  'b0;
               assign p0_rd_full        =  'b0;
                                         

       end       
       
        
 
       if (C_PORT_ENABLE[1] == 1'b1)
       begin

               assign mig_p2_arb_en      =      p1_arb_en ;
               assign mig_p2_cmd_clk     =      p1_cmd_clk  ;
               assign mig_p2_cmd_en      =      p1_cmd_en   ;
               assign mig_p2_cmd_ra      =      p1_cmd_ra  ;
               assign mig_p2_cmd_ba      =      p1_cmd_ba   ;
               assign mig_p2_cmd_ca      =      p1_cmd_ca  ;
               assign mig_p2_cmd_instr   =      p1_cmd_instr;
               assign mig_p2_cmd_bl      =      {(p1_cmd_instr[2] | p1_cmd_bl[5]),p1_cmd_bl[4:0]}  ;
               assign p1_cmd_empty       =      mig_p2_cmd_empty;  
               assign p1_cmd_full        =      mig_p2_cmd_full;   

               assign mig_p2_clk         = p1_rd_clk;
               assign mig_p3_clk         = p1_wr_clk;

               assign mig_p3_en       = p1_wr_en;
               assign mig_p3_wr_data  = p1_wr_data[31:0];
               assign mig_p3_wr_mask  = p1_wr_mask[3:0];
               assign mig_p2_en       = p1_rd_en;

               assign p1_rd_data        = mig_p2_rd_data;
               assign p1_wr_count       = mig_p3_count;
               assign p1_rd_count       = mig_p2_count;
               assign p1_wr_empty       = mig_p3_empty;
               assign p1_wr_error       = mig_p3_error;                 
               assign p1_wr_full        = mig_p3_full;
               assign p1_wr_underrun    = mig_p3_underrun;
               assign p1_rd_overflow    = mig_p2_overflow; 
               assign p1_rd_error       = mig_p2_error;
               assign p1_rd_full        = mig_p2_full;
               assign p1_rd_empty       = mig_p2_empty;
 
       end else
       begin

               assign mig_p2_arb_en      =     'b0; 
               assign mig_p2_cmd_clk     =     'b0; 
               assign mig_p2_cmd_en      =     'b0; 
               assign mig_p2_cmd_ra      =     'b0; 
               assign mig_p2_cmd_ba      =     'b0; 
               assign mig_p2_cmd_ca      =     'b0; 
               assign mig_p2_cmd_instr   =     'b0; 
               assign mig_p2_cmd_bl      =     'b0; 
               assign p1_cmd_empty       =     'b0; 
               assign p1_cmd_full        =     'b0; 
               assign mig_p3_en       = 'b0; 
               assign mig_p3_wr_data  = 'b0; 
               assign mig_p3_wr_mask  = 'b0; 
               assign mig_p2_en       = 'b0; 

               assign mig_p2_clk   = 'b0; 
               assign mig_p3_clk   = 'b0; 
               
               assign p1_rd_data        = 'b0; 
               assign p1_wr_count       = 'b0; 
               assign p1_rd_count       = 'b0; 
               assign p1_wr_empty       = 'b0; 
               assign p1_wr_error       = 'b0;         
               assign p1_wr_full        = 'b0; 
               assign p1_wr_underrun    = 'b0; 
               assign p1_rd_overflow    = 'b0; 
               assign p1_rd_error       = 'b0; 
               assign p1_rd_full        = 'b0; 
               assign p1_rd_empty       = 'b0; 
 
       end
       
       if (C_PORT_ENABLE[2] == 1'b1)
       begin
               assign mig_p4_arb_en      = p2_arb_en ;
               assign mig_p4_cmd_clk     = p2_cmd_clk  ;
               assign mig_p4_cmd_en      = p2_cmd_en   ;
               assign mig_p4_cmd_ra      = p2_cmd_ra  ;
               assign mig_p4_cmd_ba      = p2_cmd_ba   ;
               assign mig_p4_cmd_ca      = p2_cmd_ca  ;
               assign mig_p4_cmd_instr   = p2_cmd_instr;
               assign mig_p4_cmd_bl      = {(p2_cmd_instr[2] | p2_cmd_bl[5]),p2_cmd_bl[4:0]}   ;
               assign p2_cmd_empty       = mig_p4_cmd_empty ; 
               assign p2_cmd_full        = mig_p4_cmd_full  ; 
               assign mig_p5_en          = p2_wr_en;
               assign mig_p5_wr_data     = p2_wr_data[31:0];
               assign mig_p5_wr_mask     = p2_wr_mask[3:0];
               assign mig_p4_en          = p2_rd_en;
               
                assign mig_p4_clk        = p2_rd_clk;
                assign mig_p5_clk        = p2_wr_clk;

                assign p2_rd_data        = mig_p4_rd_data;
                assign p2_wr_count       = mig_p5_count;
                assign p2_rd_count       = mig_p4_count;
                assign p2_wr_empty       = mig_p5_empty;
                assign p2_wr_full        = mig_p5_full;
                assign p2_wr_error       = mig_p5_error;  
                assign p2_wr_underrun    = mig_p5_underrun;
                assign p2_rd_overflow    = mig_p4_overflow;    
                assign p2_rd_error       = mig_p4_error;
                assign p2_rd_full        = mig_p4_full;
                assign p2_rd_empty       = mig_p4_empty;
               
       end else
       begin
               assign mig_p4_arb_en      = 'b0;   
               assign mig_p4_cmd_clk     = 'b0;   
               assign mig_p4_cmd_en      = 'b0;   
               assign mig_p4_cmd_ra      = 'b0;   
               assign mig_p4_cmd_ba      = 'b0;   
               assign mig_p4_cmd_ca      = 'b0;   
               assign mig_p4_cmd_instr   = 'b0;   
               assign mig_p4_cmd_bl      = 'b0;   
               assign p2_cmd_empty       = 'b0;   
               assign p2_cmd_full        = 'b0;   
               assign mig_p5_en          = 'b0; 
               assign mig_p5_wr_data     = 'b0; 
               assign mig_p5_wr_mask     = 'b0; 
               assign mig_p4_en          = 'b0; 

                assign mig_p4_clk        = 'b0; 
                assign mig_p5_clk        = 'b0; 

                assign p2_rd_data        =   'b0;   
                assign p2_wr_count       =   'b0;   
                assign p2_rd_count       =   'b0;   
                assign p2_wr_empty       =   'b0;   
                assign p2_wr_full        =   'b0;   
                assign p2_wr_error       =   'b0;   
                assign p2_wr_underrun    =   'b0;   
                assign p2_rd_overflow    =   'b0;     
                assign p2_rd_error       =   'b0;   
                assign p2_rd_full        =   'b0;   
                assign p2_rd_empty       =   'b0;   

       end 
 

              // MCB's port 1,3,5 is not used in this Config mode
               assign mig_p1_arb_en      =      1'b0;
               assign mig_p1_cmd_clk     =      1'b0;
               assign mig_p1_cmd_en      =      1'b0;
               assign mig_p1_cmd_ra      =      15'd0;
               assign mig_p1_cmd_ba      =      3'd0;
               assign mig_p1_cmd_ca      =      12'd0;
               
               assign mig_p1_cmd_instr   =      3'd0;
               assign mig_p1_cmd_bl      =      6'd0;
                
               assign mig_p3_arb_en    =      1'b0;
               assign mig_p3_cmd_clk     =      1'b0;
               assign mig_p3_cmd_en      =      1'b0;
               assign mig_p3_cmd_ra      =      15'd0;
               assign mig_p3_cmd_ba      =      3'd0;
               assign mig_p3_cmd_ca      =      12'd0;
               
               assign mig_p3_cmd_instr   =      3'd0;
               assign mig_p3_cmd_bl      =      6'd0;

               assign mig_p5_arb_en    =      1'b0;
               assign mig_p5_cmd_clk     =      1'b0;
               assign mig_p5_cmd_en      =      1'b0;
               assign mig_p5_cmd_ra      =      15'd0;
               assign mig_p5_cmd_ba      =      3'd0;
               assign mig_p5_cmd_ca      =      12'd0;
               
               assign mig_p5_cmd_instr   =      3'd0;
               assign mig_p5_cmd_bl      =      6'd0;
 


end else if(C_PORT_CONFIG == "B64_B64" ) begin : u_config_4

               // Inputs from Application CMD Port

                 if (C_PORT_ENABLE[0] == 1'b1)
                  begin
               
                       assign mig_p0_arb_en      =      p0_arb_en ;
                       assign mig_p1_arb_en      =      p0_arb_en ;
                       
                       assign mig_p0_cmd_clk     =      p0_cmd_clk  ;
                       assign mig_p0_cmd_en      =      p0_cmd_en   ;
                       assign mig_p0_cmd_ra      =      p0_cmd_ra  ;
                       assign mig_p0_cmd_ba      =      p0_cmd_ba   ;
                       assign mig_p0_cmd_ca      =      p0_cmd_ca  ;
                       assign mig_p0_cmd_instr   =      p0_cmd_instr;
                       assign mig_p0_cmd_bl      =       {(p0_cmd_instr[2] | p0_cmd_bl[5]),p0_cmd_bl[4:0]}   ;


                        assign mig_p0_wr_clk   = p0_wr_clk;
                        assign mig_p0_rd_clk   = p0_rd_clk;
                        assign mig_p1_wr_clk   = p0_wr_clk;
                        assign mig_p1_rd_clk   = p0_rd_clk;
                        
                        if (C_USR_INTERFACE_MODE == "AXI")
                           assign mig_p0_wr_en    = p0_wr_en ;
                        else
                           assign mig_p0_wr_en    = p0_wr_en & !p0_wr_full;

                        if (C_USR_INTERFACE_MODE == "AXI")
                           assign mig_p1_wr_en    = p0_wr_en ;
                        else
                           assign mig_p1_wr_en    = p0_wr_en & !p0_wr_full;
                        
                        
                        assign mig_p0_wr_data  = p0_wr_data[31:0];
                        assign mig_p0_wr_mask  = p0_wr_mask[3:0];
                        assign mig_p1_wr_data  = p0_wr_data[63 : 32];
                        assign mig_p1_wr_mask  = p0_wr_mask[7 : 4];    


                        if (C_USR_INTERFACE_MODE == "AXI")
                           assign mig_p0_rd_en    = p0_rd_en ;
                        else
                           assign mig_p0_rd_en    = p0_rd_en & !p0_rd_empty;

                        if (C_USR_INTERFACE_MODE == "AXI")
                           assign mig_p1_rd_en    = p0_rd_en ;
                        else
                           assign mig_p1_rd_en    = p0_rd_en & !p0_rd_empty;
                        
                        assign p0_rd_data     = {mig_p1_rd_data , mig_p0_rd_data};
                        
                        assign p0_cmd_empty   =     mig_p0_cmd_empty ;
                        assign p0_cmd_full    =     mig_p0_cmd_full  ;
                        assign p0_wr_empty    = mig_p1_wr_empty;      
                        assign p0_wr_full    = mig_p1_wr_full;
                        assign p0_wr_error    = mig_p1_wr_error | mig_p0_wr_error; 
                        assign p0_wr_count    = mig_p1_wr_count;
                        assign p0_rd_count    = mig_p1_rd_count;
                        assign p0_wr_underrun = mig_p1_wr_underrun | mig_p0_wr_underrun; 
                        assign p0_rd_overflow = mig_p1_rd_overflow | mig_p0_rd_overflow; 
                        assign p0_rd_error    = mig_p1_rd_error | mig_p0_rd_error; 
                        assign p0_rd_full     = mig_p1_rd_full;
                        assign p0_rd_empty    = mig_p1_rd_empty;
                       
                       
                 end else
                 begin
                       assign mig_p0_arb_en      =      'b0;
                       assign mig_p0_cmd_clk     =      'b0;
                       assign mig_p0_cmd_en      =      'b0;
                       assign mig_p0_cmd_ra      =      'b0;
                       assign mig_p0_cmd_ba      =      'b0;
                       assign mig_p0_cmd_ca      =      'b0;
                       assign mig_p0_cmd_instr   =      'b0;
                       assign mig_p0_cmd_bl      =      'b0;

                        assign mig_p0_wr_clk   = 'b0;
                        assign mig_p0_rd_clk   = 'b0;
                        assign mig_p1_wr_clk   = 'b0;
                        assign mig_p1_rd_clk   = 'b0;
                        assign mig_p0_wr_en    = 'b0;
                        assign mig_p1_wr_en    = 'b0;
                        assign mig_p0_wr_data  = 'b0;
                        assign mig_p0_wr_mask  = 'b0;
                        assign mig_p1_wr_data  = 'b0;
                        assign mig_p1_wr_mask  = 'b0;            
                   //     assign mig_p1_wr_en    = 'b0;
                        assign mig_p0_rd_en    = 'b0;
                        assign mig_p1_rd_en    = 'b0;
                        assign p0_rd_data     = 'b0;


                        assign p0_cmd_empty   = 'b0;
                        assign p0_cmd_full    = 'b0;
                        assign p0_wr_empty    = 'b0;
                        assign p0_wr_full     = 'b0;
                        assign p0_wr_error    = 'b0;
                        assign p0_wr_count    = 'b0;
                        assign p0_rd_count    = 'b0;
                        assign p0_wr_underrun = 'b0;  
                        assign p0_rd_overflow = 'b0;
                        assign p0_rd_error    = 'b0;
                        assign p0_rd_full     = 'b0;
                        assign p0_rd_empty    = 'b0;
                 
                 
                 end

      

                 if (C_PORT_ENABLE[1] == 1'b1)
                 begin

                       assign mig_p2_arb_en      =      p1_arb_en ;
                       
                       assign mig_p2_cmd_clk     =      p1_cmd_clk  ;
                       assign mig_p2_cmd_en      =      p1_cmd_en   ;
                       assign mig_p2_cmd_ra      =      p1_cmd_ra  ;
                       assign mig_p2_cmd_ba      =      p1_cmd_ba   ;
                       assign mig_p2_cmd_ca      =      p1_cmd_ca  ;
                       assign mig_p2_cmd_instr   =      p1_cmd_instr;
                       assign mig_p2_cmd_bl      =      {(p1_cmd_instr[2] | p1_cmd_bl[5]),p1_cmd_bl[4:0]}  ;


                        assign mig_p2_clk     = p1_rd_clk;
                        assign mig_p3_clk     = p1_wr_clk;
                        assign mig_p4_clk     = p1_rd_clk;
                        assign mig_p5_clk     = p1_wr_clk;
                         
                        
                        if (C_USR_INTERFACE_MODE == "AXI")
                           assign mig_p3_en    = p1_wr_en ;
                        else
                           assign mig_p3_en    = p1_wr_en & !p1_wr_full;

                        if (C_USR_INTERFACE_MODE == "AXI")
                           assign mig_p5_en    = p1_wr_en ;
                        else
                           assign mig_p5_en    = p1_wr_en & !p1_wr_full;
                        


                        
                        
                        assign mig_p3_wr_data  = p1_wr_data[31:0];
                        assign mig_p3_wr_mask  = p1_wr_mask[3:0];
                        assign mig_p5_wr_data  = p1_wr_data[63 : 32];
                        assign mig_p5_wr_mask  = p1_wr_mask[7 : 4];                       

                        if (C_USR_INTERFACE_MODE == "AXI")
                           assign mig_p2_en    = p1_rd_en ;
                        else
                           assign mig_p2_en    = p1_rd_en & !p1_rd_empty;

                        if (C_USR_INTERFACE_MODE == "AXI")
                           assign mig_p4_en    = p1_rd_en ;
                        else
                           assign mig_p4_en    = p1_rd_en & !p1_rd_empty;


                        assign p1_cmd_empty       =      mig_p2_cmd_empty ;  
                        assign p1_cmd_full        =      mig_p2_cmd_full  ;

                        assign p1_wr_count    = mig_p5_count;
                        assign p1_rd_count    = mig_p4_count;
                        assign p1_wr_full    = mig_p5_full;
                        assign p1_wr_error    = mig_p5_error | mig_p5_error;
                        assign p1_wr_empty    = mig_p5_empty;
                        assign p1_wr_underrun = mig_p3_underrun | mig_p5_underrun;
                        assign p1_rd_overflow = mig_p4_overflow;
                        assign p1_rd_error    = mig_p4_error;
                        assign p1_rd_full     = mig_p4_full;
                        assign p1_rd_empty    = mig_p4_empty;

                        assign p1_rd_data     = {mig_p4_rd_data , mig_p2_rd_data};
                       
                       
                 end else
                 begin
                       assign mig_p2_arb_en      = 'b0;
                   //    assign mig_p3_arb_en      = 'b0;
                  //     assign mig_p4_arb_en      = 'b0;
                  //     assign mig_p5_arb_en      = 'b0;
                       
                       assign mig_p2_cmd_clk     = 'b0;
                       assign mig_p2_cmd_en      = 'b0;
                       assign mig_p2_cmd_ra      = 'b0;
                       assign mig_p2_cmd_ba      = 'b0;
                       assign mig_p2_cmd_ca      = 'b0;
                       assign mig_p2_cmd_instr   = 'b0;
                       assign mig_p2_cmd_bl      = 'b0;
                       assign mig_p2_clk      = 'b0;
                       assign mig_p3_clk      = 'b0;
                       assign mig_p4_clk      = 'b0;
                       assign mig_p5_clk      = 'b0;
                       assign mig_p3_en       = 'b0;
                       assign mig_p5_en       = 'b0;
                       assign mig_p3_wr_data  = 'b0;
                       assign mig_p3_wr_mask  = 'b0;
                       assign mig_p5_wr_data  = 'b0;
                       assign mig_p5_wr_mask  = 'b0; 
                       assign mig_p2_en    = 'b0;
                       assign mig_p4_en    = 'b0;
                       assign p1_cmd_empty    = 'b0;  
                       assign p1_cmd_full     = 'b0;  

                       assign p1_wr_count    = 'b0;
                       assign p1_rd_count    = 'b0;
                       assign p1_wr_full     = 'b0;
                       assign p1_wr_error    = 'b0;
                       assign p1_wr_empty    = 'b0;
                       assign p1_wr_underrun = 'b0;
                       assign p1_rd_overflow = 'b0;
                       assign p1_rd_error    = 'b0; 
                       assign p1_rd_full     = 'b0; 
                       assign p1_rd_empty    = 'b0; 
                       assign p1_rd_data     = 'b0;
                       
                 end               
                
                  // unused MCB's signals in this configuration
                       assign mig_p3_arb_en      =      1'b0;
                       assign mig_p4_arb_en      =      1'b0;
                       assign mig_p5_arb_en      =      1'b0;
                       
                       assign mig_p3_cmd_clk     =      1'b0;
                       assign mig_p3_cmd_en      =      1'b0;
                       assign mig_p3_cmd_ra      =      15'd0;
                       assign mig_p3_cmd_ba      =      3'd0;
                       assign mig_p3_cmd_ca      =      12'd0;
                       assign mig_p3_cmd_instr   =      3'd0;

                       assign mig_p4_cmd_clk     =      1'b0;
                       assign mig_p4_cmd_en      =      1'b0;
                       assign mig_p4_cmd_ra      =      15'd0;
                       assign mig_p4_cmd_ba      =      3'd0;
                       assign mig_p4_cmd_ca      =      12'd0;
                       assign mig_p4_cmd_instr   =      3'd0;
                       assign mig_p4_cmd_bl      =      6'd0;

                       assign mig_p5_cmd_clk     =      1'b0;
                       assign mig_p5_cmd_en      =      1'b0;
                       assign mig_p5_cmd_ra      =      15'd0;
                       assign mig_p5_cmd_ba      =      3'd0;
                       assign mig_p5_cmd_ca      =      12'd0;                       
                       assign mig_p5_cmd_instr   =      3'd0;
                       assign mig_p5_cmd_bl      =      6'd0;

                
                

  end else if(C_PORT_CONFIG == "B128" ) begin : u_config_5
//*******************************BEGIN OF CONFIG 5 SIGNALS ********************************     

               // Inputs from Application CMD Port
               
               assign mig_p0_arb_en      =  p0_arb_en ;
               assign mig_p0_cmd_clk     =  p0_cmd_clk  ;
               assign mig_p0_cmd_en      =  p0_cmd_en   ;
               assign mig_p0_cmd_ra      =  p0_cmd_ra  ;
               assign mig_p0_cmd_ba      =  p0_cmd_ba   ;
               assign mig_p0_cmd_ca      =  p0_cmd_ca  ;
               assign mig_p0_cmd_instr   =  p0_cmd_instr;
               assign mig_p0_cmd_bl      =   {(p0_cmd_instr[2] | p0_cmd_bl[5]),p0_cmd_bl[4:0]}   ;
               
               assign p0_cmd_empty       =      mig_p0_cmd_empty ;
               assign p0_cmd_full        =      mig_p0_cmd_full  ;
               
 
 
                // Inputs from Application User Port
                
                assign mig_p0_wr_clk   = p0_wr_clk;
                assign mig_p0_rd_clk   = p0_rd_clk;
                assign mig_p1_wr_clk   = p0_wr_clk;
                assign mig_p1_rd_clk   = p0_rd_clk;
                
                assign mig_p2_clk   = p0_rd_clk;
                assign mig_p3_clk   = p0_wr_clk;
                assign mig_p4_clk   = p0_rd_clk;
                assign mig_p5_clk   = p0_wr_clk;
                
                
                if (C_USR_INTERFACE_MODE == "AXI") begin
                
                   assign mig_p0_wr_en    = p0_wr_en ;
                   assign mig_p1_wr_en    = p0_wr_en ;
                   assign mig_p3_en       = p0_wr_en ;
                   assign mig_p5_en       = p0_wr_en ;
                   end
                else begin
                        
                   assign mig_p0_wr_en    = p0_wr_en & !p0_wr_full;
                   assign mig_p1_wr_en    = p0_wr_en & !p0_wr_full;
                   assign mig_p3_en       = p0_wr_en & !p0_wr_full;
                   assign mig_p5_en       = p0_wr_en & !p0_wr_full;
                end        

                
                
                
                assign mig_p0_wr_data = p0_wr_data[31:0];
                assign mig_p0_wr_mask = p0_wr_mask[3:0];
                assign mig_p1_wr_data = p0_wr_data[63 : 32];
                assign mig_p1_wr_mask = p0_wr_mask[7 : 4];                
                assign mig_p3_wr_data = p0_wr_data[95 : 64];
                assign mig_p3_wr_mask = p0_wr_mask[11 : 8];
                assign mig_p5_wr_data = p0_wr_data[127 : 96];
                assign mig_p5_wr_mask = p0_wr_mask[15 : 12];
                
                if (C_USR_INTERFACE_MODE == "AXI") begin
                    assign mig_p0_rd_en    = p0_rd_en;
                    assign mig_p1_rd_en    = p0_rd_en;
                    assign mig_p2_en       = p0_rd_en;
                    assign mig_p4_en       = p0_rd_en;
                    end
                else begin
                    assign mig_p0_rd_en    = p0_rd_en & !p0_rd_empty;
                    assign mig_p1_rd_en    = p0_rd_en & !p0_rd_empty;
                    assign mig_p2_en       = p0_rd_en & !p0_rd_empty;
                    assign mig_p4_en       = p0_rd_en & !p0_rd_empty;
                end
                
                // outputs to Applications User Port
                assign p0_rd_data     = {mig_p4_rd_data , mig_p2_rd_data , mig_p1_rd_data , mig_p0_rd_data};
                assign p0_rd_empty    = mig_p4_empty;
                assign p0_rd_full     = mig_p4_full;
                assign p0_rd_error    = mig_p0_rd_error | mig_p1_rd_error | mig_p2_error | mig_p4_error;  
                assign p0_rd_overflow    = mig_p0_rd_overflow | mig_p1_rd_overflow | mig_p2_overflow | mig_p4_overflow; 

                assign p0_wr_underrun    = mig_p0_wr_underrun | mig_p1_wr_underrun | mig_p3_underrun | mig_p5_underrun;      
                assign p0_wr_empty    = mig_p5_empty;
                assign p0_wr_full     = mig_p5_full;
                assign p0_wr_error    = mig_p0_wr_error | mig_p1_wr_error | mig_p3_error | mig_p5_error; 
                
                assign p0_wr_count    = mig_p5_count;
                assign p0_rd_count    = mig_p4_count;


               // unused MCB's siganls in this configuration
               
               assign mig_p1_arb_en      =      1'b0;
               assign mig_p1_cmd_clk     =      1'b0;
               assign mig_p1_cmd_en      =      1'b0;
               assign mig_p1_cmd_ra      =      15'd0;
               assign mig_p1_cmd_ba      =      3'd0;
               assign mig_p1_cmd_ca      =      12'd0;
               
               assign mig_p1_cmd_instr   =      3'd0;
               assign mig_p1_cmd_bl      =      6'd0;
               
               assign mig_p2_arb_en    =      1'b0;
               assign mig_p2_cmd_clk     =      1'b0;
               assign mig_p2_cmd_en      =      1'b0;
               assign mig_p2_cmd_ra      =      15'd0;
               assign mig_p2_cmd_ba      =      3'd0;
               assign mig_p2_cmd_ca      =      12'd0;
               
               assign mig_p2_cmd_instr   =      3'd0;
               assign mig_p2_cmd_bl      =      6'd0;
               
               assign mig_p3_arb_en    =      1'b0;
               assign mig_p3_cmd_clk     =      1'b0;
               assign mig_p3_cmd_en      =      1'b0;
               assign mig_p3_cmd_ra      =      15'd0;
               assign mig_p3_cmd_ba      =      3'd0;
               assign mig_p3_cmd_ca      =      12'd0;
               
               assign mig_p3_cmd_instr   =      3'd0;
               assign mig_p3_cmd_bl      =      6'd0;
               
               assign mig_p4_arb_en    =      1'b0;
               assign mig_p4_cmd_clk     =      1'b0;
               assign mig_p4_cmd_en      =      1'b0;
               assign mig_p4_cmd_ra      =      15'd0;
               assign mig_p4_cmd_ba      =      3'd0;
               assign mig_p4_cmd_ca      =      12'd0;
               
               assign mig_p4_cmd_instr   =      3'd0;
               assign mig_p4_cmd_bl      =      6'd0;
               
               assign mig_p5_arb_en    =      1'b0;
               assign mig_p5_cmd_clk     =      1'b0;
               assign mig_p5_cmd_en      =      1'b0;
               assign mig_p5_cmd_ra      =      15'd0;
               assign mig_p5_cmd_ba      =      3'd0;
               assign mig_p5_cmd_ca      =      12'd0;
               
               assign mig_p5_cmd_instr   =      3'd0;
               assign mig_p5_cmd_bl      =      6'd0;
                             
//*******************************END OF CONFIG 5 SIGNALS ********************************     
                                
end
endgenerate
                              
   MCB 
   # (         .PORT_CONFIG             (C_PORT_CONFIG),                                    
               .MEM_WIDTH              (C_NUM_DQ_PINS    ),        
               .MEM_TYPE                (C_MEM_TYPE       ), 
               .MEM_BURST_LEN            (C_MEM_BURST_LEN  ),  
               .MEM_ADDR_ORDER           (C_MEM_ADDR_ORDER),               
               .MEM_CAS_LATENCY          (C_MEM_CAS_LATENCY),        
               .MEM_DDR3_CAS_LATENCY      (C_MEM_DDR3_CAS_LATENCY   ),
               .MEM_DDR2_WRT_RECOVERY     (C_MEM_DDR2_WRT_RECOVERY  ),
               .MEM_DDR3_WRT_RECOVERY     (C_MEM_DDR3_WRT_RECOVERY  ),
               .MEM_MOBILE_PA_SR          (C_MEM_MOBILE_PA_SR       ),
               .MEM_DDR1_2_ODS              (C_MEM_DDR1_2_ODS         ),
               .MEM_DDR3_ODS                (C_MEM_DDR3_ODS           ),
               .MEM_DDR2_RTT                (C_MEM_DDR2_RTT           ),
               .MEM_DDR3_RTT                (C_MEM_DDR3_RTT           ),
               .MEM_DDR3_ADD_LATENCY        (C_MEM_DDR3_ADD_LATENCY   ),
               .MEM_DDR2_ADD_LATENCY        (C_MEM_DDR2_ADD_LATENCY   ),
               .MEM_MOBILE_TC_SR            (C_MEM_MOBILE_TC_SR       ),
               .MEM_MDDR_ODS                (C_MEM_MDDR_ODS           ),
               .MEM_DDR2_DIFF_DQS_EN        (C_MEM_DDR2_DIFF_DQS_EN   ),
               .MEM_DDR2_3_PA_SR            (C_MEM_DDR2_3_PA_SR       ),
               .MEM_DDR3_CAS_WR_LATENCY    (C_MEM_DDR3_CAS_WR_LATENCY),
               .MEM_DDR3_AUTO_SR           (C_MEM_DDR3_AUTO_SR       ),
               .MEM_DDR2_3_HIGH_TEMP_SR    (C_MEM_DDR2_3_HIGH_TEMP_SR),
               .MEM_DDR3_DYN_WRT_ODT       (C_MEM_DDR3_DYN_WRT_ODT   ),
               .MEM_RA_SIZE               (C_MEM_ADDR_WIDTH            ),
               .MEM_BA_SIZE               (C_MEM_BANKADDR_WIDTH            ),
               .MEM_CA_SIZE               (C_MEM_NUM_COL_BITS            ),
               .MEM_RAS_VAL               (MEM_RAS_VAL            ),  
               .MEM_RCD_VAL               (MEM_RCD_VAL            ),  
               .MEM_REFI_VAL               (MEM_REFI_VAL           ),  
               .MEM_RFC_VAL               (MEM_RFC_VAL            ),  
               .MEM_RP_VAL                (MEM_RP_VAL             ),  
               .MEM_WR_VAL                (MEM_WR_VAL             ),  
               .MEM_RTP_VAL               (MEM_RTP_VAL            ),  
               .MEM_WTR_VAL               (MEM_WTR_VAL            ),
               .CAL_BYPASS        (C_MC_CALIB_BYPASS),      
               .CAL_RA            (C_MC_CALIBRATION_RA),     
               .CAL_BA            (C_MC_CALIBRATION_BA ),    
               .CAL_CA            (C_MC_CALIBRATION_CA),  
               .CAL_CLK_DIV        (C_MC_CALIBRATION_CLK_DIV),        
               .CAL_DELAY         (C_MC_CALIBRATION_DELAY),
               .ARB_NUM_TIME_SLOTS         (C_ARB_NUM_TIME_SLOTS),
               .ARB_TIME_SLOT_0            (arbtimeslot0 )     ,    
               .ARB_TIME_SLOT_1            (arbtimeslot1 )     ,    
               .ARB_TIME_SLOT_2            (arbtimeslot2 )     ,    
               .ARB_TIME_SLOT_3            (arbtimeslot3 )     ,    
               .ARB_TIME_SLOT_4            (arbtimeslot4 )     ,    
               .ARB_TIME_SLOT_5            (arbtimeslot5 )     ,    
               .ARB_TIME_SLOT_6            (arbtimeslot6 )     ,    
               .ARB_TIME_SLOT_7            (arbtimeslot7 )     ,    
               .ARB_TIME_SLOT_8            (arbtimeslot8 )     ,    
               .ARB_TIME_SLOT_9            (arbtimeslot9 )     ,    
               .ARB_TIME_SLOT_10           (arbtimeslot10)   ,         
               .ARB_TIME_SLOT_11           (arbtimeslot11)            
             )  samc_0                                                
     (                                                              
                                                                    
             // HIGH-SPEED PLL clock interface
             
             .PLLCLK            ({ioclk90,ioclk0}),
             .PLLCE              ({pll_ce_90,pll_ce_0})       ,

             .PLLLOCK       (1'b1),
             
             // DQS CLOCK NETWork interface
             
             .DQSIOIN           (idelay_dqs_ioi_s),
             .DQSIOIP           (idelay_dqs_ioi_m),
             .UDQSIOIN          (idelay_udqs_ioi_s),
             .UDQSIOIP          (idelay_udqs_ioi_m),


               //.DQSPIN    (in_pre_dqsp),
               .DQI       (in_dq),
             // RESETS - GLOBAl & local
             .SYSRST         (MCB_SYSRST ), 
   
            // command port 0
             .P0ARBEN            (mig_p0_arb_en),
             .P0CMDCLK           (mig_p0_cmd_clk),
             .P0CMDEN            (mig_p0_cmd_en),
             .P0CMDRA            (mig_p0_cmd_ra),
             .P0CMDBA            (mig_p0_cmd_ba),
             .P0CMDCA            (mig_p0_cmd_ca),
             
             .P0CMDINSTR         (mig_p0_cmd_instr),
             .P0CMDBL            (mig_p0_cmd_bl),
             .P0CMDEMPTY         (mig_p0_cmd_empty),
             .P0CMDFULL          (mig_p0_cmd_full),
             
             // command port 1 
            
             .P1ARBEN            (mig_p1_arb_en),
             .P1CMDCLK           (mig_p1_cmd_clk),
             .P1CMDEN            (mig_p1_cmd_en),
             .P1CMDRA            (mig_p1_cmd_ra),
             .P1CMDBA            (mig_p1_cmd_ba),
             .P1CMDCA            (mig_p1_cmd_ca),
             
             .P1CMDINSTR         (mig_p1_cmd_instr),
             .P1CMDBL            (mig_p1_cmd_bl),
             .P1CMDEMPTY         (mig_p1_cmd_empty),
             .P1CMDFULL          (mig_p1_cmd_full),

             // command port 2
             
             .P2ARBEN            (mig_p2_arb_en),
             .P2CMDCLK           (mig_p2_cmd_clk),
             .P2CMDEN            (mig_p2_cmd_en),
             .P2CMDRA            (mig_p2_cmd_ra),
             .P2CMDBA            (mig_p2_cmd_ba),
             .P2CMDCA            (mig_p2_cmd_ca),
             
             .P2CMDINSTR         (mig_p2_cmd_instr),
             .P2CMDBL            (mig_p2_cmd_bl),
             .P2CMDEMPTY         (mig_p2_cmd_empty),
             .P2CMDFULL          (mig_p2_cmd_full),

             // command port 3
             
             .P3ARBEN            (mig_p3_arb_en),
             .P3CMDCLK           (mig_p3_cmd_clk),
             .P3CMDEN            (mig_p3_cmd_en),
             .P3CMDRA            (mig_p3_cmd_ra),
             .P3CMDBA            (mig_p3_cmd_ba),
             .P3CMDCA            (mig_p3_cmd_ca),
                               
             .P3CMDINSTR         (mig_p3_cmd_instr),
             .P3CMDBL            (mig_p3_cmd_bl),
             .P3CMDEMPTY         (mig_p3_cmd_empty),
             .P3CMDFULL          (mig_p3_cmd_full),

             // command port 4  // don't care in config 2
             
             .P4ARBEN            (mig_p4_arb_en),
             .P4CMDCLK           (mig_p4_cmd_clk),
             .P4CMDEN            (mig_p4_cmd_en),
             .P4CMDRA            (mig_p4_cmd_ra),
             .P4CMDBA            (mig_p4_cmd_ba),
             .P4CMDCA            (mig_p4_cmd_ca),
                               
             .P4CMDINSTR         (mig_p4_cmd_instr),
             .P4CMDBL            (mig_p4_cmd_bl),
             .P4CMDEMPTY         (mig_p4_cmd_empty),
             .P4CMDFULL          (mig_p4_cmd_full),

             // command port 5 // don't care in config 2
             
             .P5ARBEN            (mig_p5_arb_en),
             .P5CMDCLK           (mig_p5_cmd_clk),
             .P5CMDEN            (mig_p5_cmd_en),
             .P5CMDRA            (mig_p5_cmd_ra),
             .P5CMDBA            (mig_p5_cmd_ba),
             .P5CMDCA            (mig_p5_cmd_ca),
                               
             .P5CMDINSTR         (mig_p5_cmd_instr),
             .P5CMDBL            (mig_p5_cmd_bl),
             .P5CMDEMPTY         (mig_p5_cmd_empty),
             .P5CMDFULL          (mig_p5_cmd_full),

              
             // IOI & IOB SIGNals/tristate interface
             
             .DQIOWEN0        (dqIO_w_en_0),
             .DQSIOWEN90P     (dqsIO_w_en_90_p),
             .DQSIOWEN90N     (dqsIO_w_en_90_n),
             
             
             // IOB MEMORY INTerface signals
             .ADDR         (address_90),  
             .BA           (ba_90 ),      
             .RAS         (ras_90 ),     
             .CAS         (cas_90 ),     
             .WE          (we_90  ),     
             .CKE          (cke_90 ),     
             .ODT          (odt_90 ),     
             .RST          (rst_90 ),     
             
             // CALIBRATION DRP interface
             .IOIDRPCLK           (ioi_drp_clk    ),
             .IOIDRPADDR          (ioi_drp_addr   ),
             .IOIDRPSDO           (ioi_drp_sdo    ), 
             .IOIDRPSDI           (ioi_drp_sdi    ), 
             .IOIDRPCS            (ioi_drp_cs     ),
             .IOIDRPADD           (ioi_drp_add    ), 
             .IOIDRPBROADCAST     (ioi_drp_broadcast  ),
             .IOIDRPTRAIN         (ioi_drp_train    ),
             .IOIDRPUPDATE         (ioi_drp_update) ,
             
             // CALIBRATION DAtacapture interface
             //SPECIAL COMMANDs
             .RECAL               (mcb_recal    ), 
             .UIREAD               (mcb_ui_read),
             .UIADD                (mcb_ui_add)    ,
             .UICS                 (mcb_ui_cs)     ,
             .UICLK                (mcb_ui_clk)    ,
             .UISDI                (mcb_ui_sdi)    ,
             .UIADDR               (mcb_ui_addr)   ,
             .UIBROADCAST          (mcb_ui_broadcast) ,
             .UIDRPUPDATE          (mcb_ui_drp_update) ,
             .UIDONECAL            (mcb_ui_done_cal)   ,
             .UICMD                (mcb_ui_cmd),
             .UICMDIN              (mcb_ui_cmd_in)     ,
             .UICMDEN              (mcb_ui_cmd_en)     ,
             .UIDQCOUNT            (mcb_ui_dqcount)    ,
             .UIDQLOWERDEC          (mcb_ui_dq_lower_dec),
             .UIDQLOWERINC          (mcb_ui_dq_lower_inc),
             .UIDQUPPERDEC          (mcb_ui_dq_upper_dec),
             .UIDQUPPERINC          (mcb_ui_dq_upper_inc),
             .UIUDQSDEC          (mcb_ui_udqs_dec),
             .UIUDQSINC          (mcb_ui_udqs_inc),
             .UILDQSDEC          (mcb_ui_ldqs_dec),
             .UILDQSINC          (mcb_ui_ldqs_inc),
             .UODATA             (uo_data),
             .UODATAVALID          (uo_data_valid),
             .UODONECAL            (hard_done_cal)  ,
             .UOCMDREADYIN         (uo_cmd_ready_in),
             .UOREFRSHFLAG         (uo_refrsh_flag),
             .UOCALSTART           (uo_cal_start)   ,
             .UOSDO                (uo_sdo),
                                                   
             //CONTROL SIGNALS
              .STATUS                    (status),
              .SELFREFRESHENTER          (selfrefresh_mcb_enter  ),
              .SELFREFRESHMODE           (selfrefresh_mcb_mode ),  
//////////////////////////////  //////////////////
//MUIs
////////////////////////////////////////////////
            
              .P0RDDATA         ( mig_p0_rd_data[31:0]    ), 
              .P1RDDATA         ( mig_p1_rd_data[31:0]   ), 
              .P2RDDATA         ( mig_p2_rd_data[31:0]  ), 
              .P3RDDATA         ( mig_p3_rd_data[31:0]       ),
              .P4RDDATA         ( mig_p4_rd_data[31:0] ), 
              .P5RDDATA         ( mig_p5_rd_data[31:0]        ), 
              .LDMN             ( dqnlm       ),
              .UDMN             ( dqnum       ),
              .DQON             ( dqo_n       ),
              .DQOP             ( dqo_p       ),
              .LDMP             ( dqplm       ),
              .UDMP             ( dqpum       ),
              
              .P0RDCOUNT          ( mig_p0_rd_count ), 
              .P0WRCOUNT          ( mig_p0_wr_count ),
              .P1RDCOUNT          ( mig_p1_rd_count ), 
              .P1WRCOUNT          ( mig_p1_wr_count ), 
              .P2COUNT           ( mig_p2_count  ), 
              .P3COUNT           ( mig_p3_count  ),
              .P4COUNT           ( mig_p4_count  ),
              .P5COUNT           ( mig_p5_count  ),
              
              // NEW ADDED FIFo status siganls
              // MIG USER PORT 0
              .P0RDEMPTY        ( mig_p0_rd_empty), 
              .P0RDFULL         ( mig_p0_rd_full), 
              .P0RDOVERFLOW     ( mig_p0_rd_overflow), 
              .P0WREMPTY        ( mig_p0_wr_empty), 
              .P0WRFULL         ( mig_p0_wr_full), 
              .P0WRUNDERRUN     ( mig_p0_wr_underrun), 
              // MIG USER PORT 1
              .P1RDEMPTY        ( mig_p1_rd_empty), 
              .P1RDFULL         ( mig_p1_rd_full), 
              .P1RDOVERFLOW     ( mig_p1_rd_overflow),  
              .P1WREMPTY        ( mig_p1_wr_empty), 
              .P1WRFULL         ( mig_p1_wr_full), 
              .P1WRUNDERRUN     ( mig_p1_wr_underrun),  
              
              // MIG USER PORT 2
              .P2EMPTY          ( mig_p2_empty),
              .P2FULL           ( mig_p2_full),
              .P2RDOVERFLOW        ( mig_p2_overflow), 
              .P2WRUNDERRUN       ( mig_p2_underrun), 
              
              .P3EMPTY          ( mig_p3_empty ),
              .P3FULL           ( mig_p3_full ),
              .P3RDOVERFLOW        ( mig_p3_overflow), 
              .P3WRUNDERRUN       ( mig_p3_underrun ),
              // MIG USER PORT 3
              .P4EMPTY          ( mig_p4_empty),
              .P4FULL           ( mig_p4_full),
              .P4RDOVERFLOW        ( mig_p4_overflow), 
              .P4WRUNDERRUN       ( mig_p4_underrun), 
              
              .P5EMPTY          ( mig_p5_empty ),
              .P5FULL           ( mig_p5_full ),
              .P5RDOVERFLOW        ( mig_p5_overflow), 
              .P5WRUNDERRUN       ( mig_p5_underrun), 
              
              ////////////////////////////////////////////////////////-
              .P0WREN        ( mig_p0_wr_en), 
              .P0RDEN        ( mig_p0_rd_en),                         
              .P1WREN        ( mig_p1_wr_en), 
              .P1RDEN        ( mig_p1_rd_en), 
              .P2EN          ( mig_p2_en),
              .P3EN          ( mig_p3_en), 
              .P4EN          ( mig_p4_en), 
              .P5EN          ( mig_p5_en), 
              // WRITE  MASK BIts connection
              .P0RWRMASK        ( mig_p0_wr_mask[3:0]), 
              .P1RWRMASK        ( mig_p1_wr_mask[3:0]),
              .P2WRMASK        ( mig_p2_wr_mask[3:0]),
              .P3WRMASK        ( mig_p3_wr_mask[3:0]), 
              .P4WRMASK        ( mig_p4_wr_mask[3:0]),
              .P5WRMASK        ( mig_p5_wr_mask[3:0]), 
              // DATA WRITE COnnection
              .P0WRDATA      ( mig_p0_wr_data[31:0]), 
              .P1WRDATA      ( mig_p1_wr_data[31:0]),
              .P2WRDATA      ( mig_p2_wr_data[31:0]),
              .P3WRDATA      ( mig_p3_wr_data[31:0]), 
              .P4WRDATA      ( mig_p4_wr_data[31:0]),
              .P5WRDATA      ( mig_p5_wr_data[31:0]),  
              
              .P0WRERROR     (mig_p0_wr_error),
              .P1WRERROR     (mig_p1_wr_error),
              .P0RDERROR     (mig_p0_rd_error),
              .P1RDERROR     (mig_p1_rd_error),
              
              .P2ERROR       (mig_p2_error),
              .P3ERROR       (mig_p3_error),
              .P4ERROR       (mig_p4_error),
              .P5ERROR       (mig_p5_error),
              
              //  USER SIDE DAta ports clock
              //  128 BITS CONnections
              .P0WRCLK            ( mig_p0_wr_clk  ),
              .P1WRCLK            ( mig_p1_wr_clk  ),
              .P0RDCLK            ( mig_p0_rd_clk  ),
              .P1RDCLK            ( mig_p1_rd_clk  ),
              .P2CLK              ( mig_p2_clk  ),
              .P3CLK              ( mig_p3_clk  ),
              .P4CLK              ( mig_p4_clk  ),
              .P5CLK              ( mig_p5_clk) 
              ////////////////////////////////////////////////////////
              // TST MODE PINS
              
                            
            
              );
             

//////////////////////////////////////////////////////
// Input Termination Calibration
//////////////////////////////////////////////////////
wire                          DONE_SOFTANDHARD_CAL;

assign uo_done_cal = (   C_CALIB_SOFT_IP == "TRUE") ? DONE_SOFTANDHARD_CAL : hard_done_cal;
generate   
if ( C_CALIB_SOFT_IP == "TRUE") begin: gen_term_calib


  

 
mcb_soft_calibration_top  # (

    .C_MEM_TZQINIT_MAXCNT (C_MEM_TZQINIT_MAXCNT),
    .C_MC_CALIBRATION_MODE(C_MC_CALIBRATION_MODE),
    .SKIP_IN_TERM_CAL     (C_SKIP_IN_TERM_CAL),
    .SKIP_DYNAMIC_CAL     (C_SKIP_DYNAMIC_CAL),
    .SKIP_DYN_IN_TERM     (C_SKIP_DYN_IN_TERM),
    .C_SIMULATION         (C_SIMULATION),
    .C_MEM_TYPE           (C_MEM_TYPE)
        )
  mcb_soft_calibration_top_inst (
    .UI_CLK               (ui_clk),               //Input - global clock to be used for input_term_tuner and IODRP clock
    .RST                  (int_sys_rst),              //Input - reset for input_term_tuner - synchronous for input_term_tuner state machine, asynch for IODRP (sub)controller
    .IOCLK                (ioclk0),               //Input - IOCLK input to the IODRP's
    .DONE_SOFTANDHARD_CAL (DONE_SOFTANDHARD_CAL), // active high flag signals soft calibration of input delays is complete and MCB_UODONECAL is high (MCB hard calib complete)
    .PLL_LOCK             (gated_pll_lock),
    
    .SELFREFRESH_REQ      (soft_cal_selfrefresh_req),    // from user app
    .SELFREFRESH_MCB_MODE (selfrefresh_mcb_mode), // from MCB
    .SELFREFRESH_MCB_REQ  (selfrefresh_mcb_enter),// to mcb
    .SELFREFRESH_MODE     (selfrefresh_mode),     // to user app
    
    
    
    .MCB_UIADD            (mcb_ui_add),
    .MCB_UISDI            (mcb_ui_sdi),
    .MCB_UOSDO            (uo_sdo),               // from MCB's UOSDO port (User output SDO)
    .MCB_UODONECAL        (hard_done_cal),        // input for when MCB hard calibration process is complete
    .MCB_UOREFRSHFLAG     (uo_refrsh_flag),       //high during refresh cycle and time when MCB is innactive
    .MCB_UICS             (mcb_ui_cs),            // to MCB's UICS port (User Input CS)
    .MCB_UIDRPUPDATE      (mcb_ui_drp_update),    // MCB's UIDRPUPDATE port (gets passed to IODRP2_MCB's MEMUPDATE port: this controls shadow latch used during IODRP2_MCB writes).  Currently just trasnparent
    .MCB_UIBROADCAST      (mcb_ui_broadcast),     // to MCB's UIBROADCAST port (User Input BROADCAST - gets passed to IODRP2_MCB's BKST port)
    .MCB_UIADDR           (mcb_ui_addr),          //to MCB's UIADDR port (gets passed to IODRP2_MCB's AUXADDR port
    .MCB_UICMDEN          (mcb_ui_cmd_en),        //set to take control of UI interface - removes control from internal calib block
    .MCB_UIDONECAL        (mcb_ui_done_cal),      //
    .MCB_UIDQLOWERDEC     (mcb_ui_dq_lower_dec),
    .MCB_UIDQLOWERINC     (mcb_ui_dq_lower_inc),
    .MCB_UIDQUPPERDEC     (mcb_ui_dq_upper_dec),
    .MCB_UIDQUPPERINC     (mcb_ui_dq_upper_inc),
    .MCB_UILDQSDEC        (mcb_ui_ldqs_dec),
    .MCB_UILDQSINC        (mcb_ui_ldqs_inc),
    .MCB_UIREAD           (mcb_ui_read),          //enables read w/o writing by turning on a SDO->SDI loopback inside the IODRP2_MCBs (doesn't exist in regular IODRP2).  IODRPCTRLR_R_WB becomes don't-care.
    .MCB_UIUDQSDEC        (mcb_ui_udqs_dec),
    .MCB_UIUDQSINC        (mcb_ui_udqs_inc),
    .MCB_RECAL            (mcb_recal),
    .MCB_SYSRST           (MCB_SYSRST),           //drives the MCB's SYSRST pin - the main reset for MCB
    .MCB_UICMD            (mcb_ui_cmd),
    .MCB_UICMDIN          (mcb_ui_cmd_in),
    .MCB_UIDQCOUNT        (mcb_ui_dqcount),
    .MCB_UODATA           (uo_data),
    .MCB_UODATAVALID      (uo_data_valid),
    .MCB_UOCMDREADY       (uo_cmd_ready_in),
    .MCB_UO_CAL_START     (uo_cal_start),
    .RZQ_Pin              (rzq),
    .ZIO_Pin              (zio),
    .CKE_Train            (cke_train)
    
     );






        assign mcb_ui_clk = ui_clk;
end
endgenerate

generate   
if ( C_CALIB_SOFT_IP != "TRUE") begin: gen_no_term_calib   
    assign DONE_SOFTANDHARD_CAL = 1'b0;
    assign MCB_SYSRST = int_sys_rst | (~wait_200us_counter[15]);
    assign mcb_recal = calib_recal;
    assign mcb_ui_read = ui_read;
    assign mcb_ui_add = ui_add;
    assign mcb_ui_cs = ui_cs;  
    assign mcb_ui_clk = ui_clk;
    assign mcb_ui_sdi = ui_sdi;
    assign mcb_ui_addr = ui_addr;
    assign mcb_ui_broadcast = ui_broadcast;
    assign mcb_ui_drp_update = ui_drp_update;
    assign mcb_ui_done_cal = ui_done_cal;
    assign mcb_ui_cmd = ui_cmd;
    assign mcb_ui_cmd_in = ui_cmd_in;
    assign mcb_ui_cmd_en = ui_cmd_en;
    assign mcb_ui_dq_lower_dec = ui_dq_lower_dec;
    assign mcb_ui_dq_lower_inc = ui_dq_lower_inc;
    assign mcb_ui_dq_upper_dec = ui_dq_upper_dec;
    assign mcb_ui_dq_upper_inc = ui_dq_upper_inc;
    assign mcb_ui_udqs_inc = ui_udqs_inc;
    assign mcb_ui_udqs_dec = ui_udqs_dec;
    assign mcb_ui_ldqs_inc = ui_ldqs_inc;
    assign mcb_ui_ldqs_dec = ui_ldqs_dec; 
    assign selfrefresh_mode = 1'b0;
 
    if (C_SIMULATION == "FALSE") begin: init_sequence
        always @ (posedge ui_clk, posedge int_sys_rst)
        begin
            if (int_sys_rst)
                wait_200us_counter <= 'b0;
            else 
               if (wait_200us_counter[15])  // UI_CLK maximum is up to 100 MHz.
                   wait_200us_counter <= wait_200us_counter                        ;
               else
                   wait_200us_counter <= wait_200us_counter + 1'b1;
        end 
    end 
    else begin: init_sequence_skip
// synthesis translate_off        
        initial
        begin
           wait_200us_counter = 16'hFFFF;
           $display("The 200 us wait period required before CKE goes active has been skipped in Simulation\n");
        end       
// synthesis translate_on         
    end
   
    
    if( C_MEM_TYPE == "DDR2") begin : gen_cketrain_a

        always @ ( posedge ui_clk)
        begin 
          // When wait_200us_[13] and wait_200us_[14] are both asserted,
          // 200 us wait should have been passed. 
          if (wait_200us_counter[14] && wait_200us_counter[13])
             wait_200us_done_r1 <= 1'b1;
          else
             wait_200us_done_r1 <= 1'b0;
          

          wait_200us_done_r2 <= wait_200us_done_r1;
        end
        
        always @ ( posedge ui_clk, posedge int_sys_rst)
        begin 
        if (int_sys_rst)
           cke_train_reg <= 1'b0;
        else 
           if ( wait_200us_done_r1 && ~wait_200us_done_r2 )
               cke_train_reg <= 1'b1;
           else if ( uo_done_cal)
               cke_train_reg <= 1'b0;
        end
        
        assign cke_train = cke_train_reg;
    end

    if( C_MEM_TYPE != "DDR2") begin : gen_cketrain_b
    
        assign cke_train = 1'b0;
    end        
        
        
end 
endgenerate

//////////////////////////////////////////////////////
//ODDRDES2 instantiations
//////////////////////////////////////////////////////

////////
//ADDR
////////

genvar addr_ioi;
   generate 
      for(addr_ioi = 0; addr_ioi < C_MEM_ADDR_WIDTH; addr_ioi = addr_ioi + 1) begin : gen_addr_oserdes2
OSERDES2 #(
  .BYPASS_GCLK_FF ("TRUE"),
  .DATA_RATE_OQ  (C_OSERDES2_DATA_RATE_OQ),         // SDR, DDR      | Data Rate setting
  .DATA_RATE_OT  (C_OSERDES2_DATA_RATE_OT),         // SDR, DDR, BUF | Tristate Rate setting.
  .OUTPUT_MODE   (C_OSERDES2_OUTPUT_MODE_SE),          // SINGLE_ENDED, DIFFERENTIAL
  .SERDES_MODE   (C_OSERDES2_SERDES_MODE_MASTER),          // MASTER, SLAVE
  .DATA_WIDTH    (2)           // {1..8} 
) ioi_addr_0  
(
  .OQ(ioi_addr[addr_ioi]),
  .SHIFTOUT1(),
  .SHIFTOUT2(),
  .SHIFTOUT3(),
  .SHIFTOUT4(),
  .TQ(t_addr[addr_ioi]),
  .CLK0(ioclk0),
  .CLK1(1'b0),
  .CLKDIV(1'b0),
  .D1(address_90[addr_ioi]),
  .D2(address_90[addr_ioi]),
  .D3(1'b0),
  .D4(1'b0),
  .IOCE(pll_ce_0),
  .OCE(1'b1),
  .RST(int_sys_rst),
  .SHIFTIN1(1'b0),
  .SHIFTIN2(1'b0),
  .SHIFTIN3(1'b0),
  .SHIFTIN4(1'b0),
  .T1(1'b0),
  .T2(1'b0),
  .T3(1'b0),
  .T4(1'b0),
  .TCE(1'b1),
  .TRAIN(1'b0)
    );
 end       
   endgenerate

////////
//BA
////////

genvar ba_ioi;
   generate 
      for(ba_ioi = 0; ba_ioi < C_MEM_BANKADDR_WIDTH; ba_ioi = ba_ioi + 1) begin : gen_ba_oserdes2
OSERDES2 #(
  .BYPASS_GCLK_FF ("TRUE"),
  .DATA_RATE_OQ  (C_OSERDES2_DATA_RATE_OQ),         // SDR, DDR      | Data Rate setting
  .DATA_RATE_OT  (C_OSERDES2_DATA_RATE_OT),         // SDR, DDR, BUF | Tristate Rate setting.
  .OUTPUT_MODE   (C_OSERDES2_OUTPUT_MODE_SE),          // SINGLE_ENDED, DIFFERENTIAL
  .SERDES_MODE   (C_OSERDES2_SERDES_MODE_MASTER),          // MASTER, SLAVE
  .DATA_WIDTH    (2)           // {1..8} 
) ioi_ba_0  
(
  .OQ       (ioi_ba[ba_ioi]),
  .SHIFTOUT1 (),
  .SHIFTOUT2 (),
  .SHIFTOUT3 (),
  .SHIFTOUT4 (),
  .TQ       (t_ba[ba_ioi]),
  .CLK0     (ioclk0),
  .CLK1 (1'b0),
  .CLKDIV (1'b0),
  .D1       (ba_90[ba_ioi]),
  .D2       (ba_90[ba_ioi]),
  .D3 (1'b0),
  .D4 (1'b0),
  .IOCE     (pll_ce_0),
  .OCE      (1'b1),
  .RST      (int_sys_rst),
  .SHIFTIN1 (1'b0),
  .SHIFTIN2 (1'b0),
  .SHIFTIN3 (1'b0),
  .SHIFTIN4 (1'b0),
  .T1(1'b0),
  .T2(1'b0),
  .T3(1'b0),
  .T4(1'b0),
  .TCE(1'b1),
  .TRAIN    (1'b0)
    );
 end       
   endgenerate

////////
//CAS
////////

OSERDES2 #(
  .BYPASS_GCLK_FF ("TRUE"),
  .DATA_RATE_OQ  (C_OSERDES2_DATA_RATE_OQ),         // SDR, DDR      | Data Rate setting
  .DATA_RATE_OT  (C_OSERDES2_DATA_RATE_OT),         // SDR, DDR, BUF | Tristate Rate setting.
  .OUTPUT_MODE   (C_OSERDES2_OUTPUT_MODE_SE),          // SINGLE_ENDED, DIFFERENTIAL
  .SERDES_MODE   (C_OSERDES2_SERDES_MODE_MASTER),          // MASTER, SLAVE
  .DATA_WIDTH    (2)           // {1..8} 
) ioi_cas_0 
(
  .OQ       (ioi_cas),
  .SHIFTOUT1 (),
  .SHIFTOUT2 (),
  .SHIFTOUT3 (),
  .SHIFTOUT4 (),
  .TQ       (t_cas),
  .CLK0     (ioclk0),
  .CLK1 (1'b0),
  .CLKDIV (1'b0),
  .D1       (cas_90),
  .D2       (cas_90),
  .D3 (1'b0),
  .D4 (1'b0),
  .IOCE     (pll_ce_0),
  .OCE      (1'b1),
  .RST      (int_sys_rst),
  .SHIFTIN1 (1'b0),
  .SHIFTIN2 (1'b0),
  .SHIFTIN3 (1'b0),
  .SHIFTIN4 (1'b0),
  .T1(1'b0),
  .T2(1'b0),
  .T3(1'b0),
  .T4(1'b0),
  .TCE(1'b1),
  .TRAIN    (1'b0)
    );

////////
//CKE
////////

OSERDES2 #(
  .BYPASS_GCLK_FF ("TRUE"),
  .DATA_RATE_OQ  (C_OSERDES2_DATA_RATE_OQ),         // SDR, DDR      | Data Rate setting
  .DATA_RATE_OT  (C_OSERDES2_DATA_RATE_OT),         // SDR, DDR, BUF | Tristate Rate setting.
  .OUTPUT_MODE   (C_OSERDES2_OUTPUT_MODE_SE),          // SINGLE_ENDED, DIFFERENTIAL
  .SERDES_MODE   (C_OSERDES2_SERDES_MODE_MASTER),          // MASTER, SLAVE
  .DATA_WIDTH    (2)    ,       // {1..8} 
  .TRAIN_PATTERN (15)
) ioi_cke_0 
(
  .OQ       (ioi_cke),
  .SHIFTOUT1 (),
  .SHIFTOUT2 (),
  .SHIFTOUT3 (),
  .SHIFTOUT4 (),
  .TQ       (t_cke),
  .CLK0     (ioclk0),
  .CLK1 (1'b0),
  .CLKDIV (1'b0),
  .D1       (cke_90),
  .D2       (cke_90),
  .D3 (1'b0),
  .D4 (1'b0),
  .IOCE     (pll_ce_0),
  .OCE      (pll_lock),
  .RST      (1'b0),//int_sys_rst
  .SHIFTIN1 (1'b0),
  .SHIFTIN2 (1'b0),
  .SHIFTIN3 (1'b0),
  .SHIFTIN4 (1'b0),
  .T1(1'b0),
  .T2(1'b0),
  .T3(1'b0),
  .T4(1'b0),
  .TCE(1'b1),
  .TRAIN    (cke_train)
    );

////////
//ODT
////////
generate
if(C_MEM_TYPE == "DDR3" || C_MEM_TYPE == "DDR2" ) begin : gen_ioi_odt

OSERDES2 #(
  .BYPASS_GCLK_FF ("TRUE"),
  .DATA_RATE_OQ  (C_OSERDES2_DATA_RATE_OQ),         // SDR, DDR      | Data Rate setting
  .DATA_RATE_OT  (C_OSERDES2_DATA_RATE_OT),         // SDR, DDR, BUF | Tristate Rate setting.
  .OUTPUT_MODE   (C_OSERDES2_OUTPUT_MODE_SE),          // SINGLE_ENDED, DIFFERENTIAL
  .SERDES_MODE   (C_OSERDES2_SERDES_MODE_MASTER),          // MASTER, SLAVE
  .DATA_WIDTH    (2)           // {1..8} 
) ioi_odt_0 
(
  .OQ       (ioi_odt),
  .SHIFTOUT1 (),
  .SHIFTOUT2 (),
  .SHIFTOUT3 (),
  .SHIFTOUT4 (),
  .TQ       (t_odt),
  .CLK0     (ioclk0),
  .CLK1 (1'b0),
  .CLKDIV (1'b0),
  .D1       (odt_90),
  .D2       (odt_90),
  .D3 (1'b0),
  .D4 (1'b0),
  .IOCE     (pll_ce_0),
  .OCE      (1'b1),
  .RST      (int_sys_rst),
  .SHIFTIN1 (1'b0),
  .SHIFTIN2 (1'b0),
  .SHIFTIN3 (1'b0),
  .SHIFTIN4 (1'b0),
  .T1(1'b0),
  .T2(1'b0),
  .T3(1'b0),
  .T4(1'b0),
  .TCE(1'b1),
  .TRAIN    (1'b0)
    );
end
endgenerate
////////
//RAS
////////

OSERDES2 #(
  .BYPASS_GCLK_FF ("TRUE"),
  .DATA_RATE_OQ  (C_OSERDES2_DATA_RATE_OQ),         // SDR, DDR      | Data Rate setting
  .DATA_RATE_OT  (C_OSERDES2_DATA_RATE_OT),         // SDR, DDR, BUF | Tristate Rate setting.
  .OUTPUT_MODE   (C_OSERDES2_OUTPUT_MODE_SE),          // SINGLE_ENDED, DIFFERENTIAL
  .SERDES_MODE   (C_OSERDES2_SERDES_MODE_MASTER),          // MASTER, SLAVE
  .DATA_WIDTH    (2)           // {1..8} 
) ioi_ras_0 
(
  .OQ       (ioi_ras),
  .SHIFTOUT1 (),
  .SHIFTOUT2 (),
  .SHIFTOUT3 (),
  .SHIFTOUT4 (),
  .TQ       (t_ras),
  .CLK0     (ioclk0),
  .CLK1 (1'b0),
  .CLKDIV (1'b0),
  .D1       (ras_90),
  .D2       (ras_90),
  .D3 (1'b0),
  .D4 (1'b0),
  .IOCE     (pll_ce_0),
  .OCE      (1'b1),
  .RST      (int_sys_rst),
  .SHIFTIN1 (1'b0),
  .SHIFTIN2 (1'b0),
  .SHIFTIN3 (1'b0),
  .SHIFTIN4 (1'b0),
  .T1 (1'b0),
  .T2 (1'b0),
  .T3 (1'b0),
  .T4 (1'b0),
  .TCE (1'b1),
  .TRAIN    (1'b0)
    );

////////
//RST
////////
generate 
if (C_MEM_TYPE == "DDR3"  ) begin : gen_ioi_rst

OSERDES2 #(
  .BYPASS_GCLK_FF ("TRUE"),
  .DATA_RATE_OQ  (C_OSERDES2_DATA_RATE_OQ),         // SDR, DDR      | Data Rate setting
  .DATA_RATE_OT  (C_OSERDES2_DATA_RATE_OT),         // SDR, DDR, BUF | Tristate Rate setting.
  .OUTPUT_MODE   (C_OSERDES2_OUTPUT_MODE_SE),          // SINGLE_ENDED, DIFFERENTIAL
  .SERDES_MODE   (C_OSERDES2_SERDES_MODE_MASTER),          // MASTER, SLAVE
  .DATA_WIDTH    (2)           // {1..8} 
) ioi_rst_0 
(
  .OQ       (ioi_rst),
  .SHIFTOUT1 (),
  .SHIFTOUT2 (),
  .SHIFTOUT3 (),
  .SHIFTOUT4 (),
  .TQ       (t_rst),
  .CLK0     (ioclk0),
  .CLK1 (1'b0),
  .CLKDIV (1'b0),
  .D1       (rst_90),
  .D2       (rst_90),
  .D3 (1'b0),
  .D4 (1'b0),
  .IOCE     (pll_ce_0),
  .OCE      (pll_lock),
  .RST      (int_sys_rst),
  .SHIFTIN1 (1'b0),
  .SHIFTIN2 (1'b0),
  .SHIFTIN3 (1'b0),
  .SHIFTIN4 (1'b0),
  .T1(1'b0),
  .T2(1'b0),
  .T3(1'b0),
  .T4(1'b0),
  .TCE(1'b1),
  .TRAIN    (1'b0)
    );
end
endgenerate
////////
//WE
////////

OSERDES2 #(
  .BYPASS_GCLK_FF ("TRUE"),
  .DATA_RATE_OQ  (C_OSERDES2_DATA_RATE_OQ),         // SDR, DDR      | Data Rate setting
  .DATA_RATE_OT  (C_OSERDES2_DATA_RATE_OT),         // SDR, DDR, BUF | Tristate Rate setting.
  .OUTPUT_MODE   (C_OSERDES2_OUTPUT_MODE_SE),          // SINGLE_ENDED, DIFFERENTIAL
  .SERDES_MODE   (C_OSERDES2_SERDES_MODE_MASTER),          // MASTER, SLAVE
  .DATA_WIDTH    (2)           // {1..8} 
) ioi_we_0 
(
  .OQ       (ioi_we),
  .TQ       (t_we),
  .SHIFTOUT1 (),
  .SHIFTOUT2 (),
  .SHIFTOUT3 (),
  .SHIFTOUT4 (),
  .CLK0     (ioclk0),
  .CLK1 (1'b0),
  .CLKDIV (1'b0),
  .D1       (we_90),
  .D2       (we_90),
  .D3 (1'b0),
  .D4 (1'b0),
  .IOCE     (pll_ce_0),
  .OCE      (1'b1),
  .RST      (int_sys_rst),
  .SHIFTIN1 (1'b0),
  .SHIFTIN2 (1'b0),
  .SHIFTIN3 (1'b0),
  .SHIFTIN4 (1'b0),
  .T1(1'b0),
  .T2(1'b0),
  .T3(1'b0),
  .T4(1'b0),
  .TCE(1'b1),
  .TRAIN    (1'b0)
);

////////
//CK
////////

OSERDES2 #(
  .BYPASS_GCLK_FF ("TRUE"),
  .DATA_RATE_OQ  (C_OSERDES2_DATA_RATE_OQ),         // SDR, DDR      | Data Rate setting
  .DATA_RATE_OT  (C_OSERDES2_DATA_RATE_OT),         // SDR, DDR, BUF | Tristate Rate setting.
  .OUTPUT_MODE   (C_OSERDES2_OUTPUT_MODE_SE),          // SINGLE_ENDED, DIFFERENTIAL
  .SERDES_MODE   (C_OSERDES2_SERDES_MODE_MASTER),          // MASTER, SLAVE
  .DATA_WIDTH    (2)           // {1..8} 
) ioi_ck_0 
(
  .OQ       (ioi_ck),
  .SHIFTOUT1(),
  .SHIFTOUT2(),
  .SHIFTOUT3(),
  .SHIFTOUT4(),
  .TQ       (t_ck),
  .CLK0     (ioclk0),
  .CLK1(1'b0),
  .CLKDIV(1'b0),
  .D1       (1'b0),
  .D2       (1'b1),
  .D3(1'b0),
  .D4(1'b0),
  .IOCE     (pll_ce_0),
  .OCE      (pll_lock),

  .RST      (1'b0),//int_sys_rst
  .SHIFTIN1(1'b0),
  .SHIFTIN2(1'b0),
  .SHIFTIN3 (1'b0),
  .SHIFTIN4 (1'b0),
  .T1(1'b0),
  .T2(1'b0),
  .T3(1'b0),
  .T4(1'b0),
  .TCE(1'b1),
  .TRAIN    (1'b0)
);

////////
//CKN
////////
/*
OSERDES2 #(
  .BYPASS_GCLK_FF ("TRUE"),
  .DATA_RATE_OQ  (C_OSERDES2_DATA_RATE_OQ),         // SDR, DDR      | Data Rate setting
  .DATA_RATE_OT  (C_OSERDES2_DATA_RATE_OT),         // SDR, DDR, BUF | Tristate Rate setting.
  .OUTPUT_MODE   (C_OSERDES2_OUTPUT_MODE_SE),          // SINGLE_ENDED, DIFFERENTIAL
  .SERDES_MODE   (C_OSERDES2_SERDES_MODE_SLAVE),          // MASTER, SLAVE
  .DATA_WIDTH    (2)           // {1..8} 
) ioi_ckn_0 
(
  .OQ       (ioi_ckn),
  .SHIFTOUT1(),
  .SHIFTOUT2(),
  .SHIFTOUT3(),
  .SHIFTOUT4(),
  .TQ       (t_ckn),
  .CLK0     (ioclk0),
  .CLK1(),
  .CLKDIV(),
  .D1       (1'b1),
  .D2       (1'b0),
  .D3(),
  .D4(),
  .IOCE     (pll_ce_0),
  .OCE      (1'b1),
  .RST      (1'b0),//int_sys_rst
  .SHIFTIN1 (),
  .SHIFTIN2 (),
  .SHIFTIN3(),
  .SHIFTIN4(),
  .T1(1'b0),
  .T2(1'b0),
  .T3(),
  .T4(),
  .TCE(1'b1),
  .TRAIN    (1'b0)
);
*/

////////
//UDM
////////

wire udm_oq;
wire udm_t;
OSERDES2 #(
  .BYPASS_GCLK_FF ("TRUE"),
  .DATA_RATE_OQ  (C_OSERDES2_DATA_RATE_OQ),         // SDR, DDR      | Data Rate setting
  .DATA_RATE_OT  (C_OSERDES2_DATA_RATE_OT),         // SDR, DDR, BUF | Tristate Rate setting.
  .OUTPUT_MODE   (C_OSERDES2_OUTPUT_MODE_SE),          // SINGLE_ENDED, DIFFERENTIAL
  .SERDES_MODE   (C_OSERDES2_SERDES_MODE_MASTER),          // MASTER, SLAVE
  .DATA_WIDTH    (2)           // {1..8} 
) ioi_udm_0 
(
  .OQ       (udm_oq),
  .SHIFTOUT1 (),
  .SHIFTOUT2 (),
  .SHIFTOUT3 (),
  .SHIFTOUT4 (),
  .TQ       (udm_t),
  .CLK0     (ioclk90),
  .CLK1 (1'b0),
  .CLKDIV (1'b0),
  .D1       (dqpum),
  .D2       (dqnum),
  .D3 (1'b0),
  .D4 (1'b0),
  .IOCE     (pll_ce_90),
  .OCE      (1'b1),
  .RST      (int_sys_rst),
  .SHIFTIN1 (1'b0),
  .SHIFTIN2 (1'b0),
  .SHIFTIN3 (1'b0),
  .SHIFTIN4 (1'b0),
  .T1       (dqIO_w_en_0),
  .T2       (dqIO_w_en_0),
  .T3 (1'b0),
  .T4 (1'b0),
  .TCE      (1'b1),
  .TRAIN    (1'b0)
);

////////
//LDM
////////
wire ldm_oq;
wire ldm_t;
OSERDES2 #(
  .BYPASS_GCLK_FF ("TRUE"),
  .DATA_RATE_OQ  (C_OSERDES2_DATA_RATE_OQ),         // SDR, DDR      | Data Rate setting
  .DATA_RATE_OT  (C_OSERDES2_DATA_RATE_OT),         // SDR, DDR, BUF | Tristate Rate setting.
  .OUTPUT_MODE   (C_OSERDES2_OUTPUT_MODE_SE),          // SINGLE_ENDED, DIFFERENTIAL
  .SERDES_MODE   (C_OSERDES2_SERDES_MODE_MASTER),          // MASTER, SLAVE
  .DATA_WIDTH    (2)           // {1..8} 
) ioi_ldm_0 
(
  .OQ       (ldm_oq),
  .SHIFTOUT1 (),
  .SHIFTOUT2 (),
  .SHIFTOUT3 (),
  .SHIFTOUT4 (),
  .TQ       (ldm_t),
  .CLK0     (ioclk90),
  .CLK1 (1'b0),
  .CLKDIV (1'b0),
  .D1       (dqplm),
  .D2       (dqnlm),
  .D3 (1'b0),
  .D4 (1'b0),
  .IOCE     (pll_ce_90),
  .OCE      (1'b1),
  .RST      (int_sys_rst),
  .SHIFTIN1 (1'b0),
  .SHIFTIN2 (1'b0),
  .SHIFTIN3 (1'b0),
  .SHIFTIN4 (1'b0),
  .T1       (dqIO_w_en_0),
  .T2       (dqIO_w_en_0),
  .T3 (1'b0),
  .T4 (1'b0),
  .TCE      (1'b1),
  .TRAIN    (1'b0)
);

////////
//DQ
////////

wire dq_oq [C_NUM_DQ_PINS-1:0];
wire dq_tq [C_NUM_DQ_PINS-1:0];

genvar dq;
generate
      for(dq = 0; dq < C_NUM_DQ_PINS; dq = dq + 1) begin : gen_dq

OSERDES2 #(
  .BYPASS_GCLK_FF ("TRUE"),
  .DATA_RATE_OQ  (C_OSERDES2_DATA_RATE_OQ),         // SDR, DDR      | Data Rate setting
  .DATA_RATE_OT  (C_OSERDES2_DATA_RATE_OT),         // SDR, DDR, BUF | Tristate Rate setting.
  .OUTPUT_MODE   (C_OSERDES2_OUTPUT_MODE_SE),          // SINGLE_ENDED, DIFFERENTIAL
  .SERDES_MODE   (C_OSERDES2_SERDES_MODE_MASTER),          // MASTER, SLAVE
  .DATA_WIDTH    (2),           // {1..8} 
  .TRAIN_PATTERN (5)            // {0..15}             
) oserdes2_dq_0 
(
  .OQ       (dq_oq[dq]),
  .SHIFTOUT1 (),
  .SHIFTOUT2 (),
  .SHIFTOUT3 (),
  .SHIFTOUT4 (),
  .TQ       (dq_tq[dq]),
  .CLK0     (ioclk90),
  .CLK1 (1'b0),
  .CLKDIV (1'b0),
  .D1       (dqo_p[dq]),
  .D2       (dqo_n[dq]),
  .D3 (1'b0),
  .D4 (1'b0),
  .IOCE     (pll_ce_90),
  .OCE      (1'b1),
  .RST      (int_sys_rst),
  .SHIFTIN1 (1'b0),
  .SHIFTIN2 (1'b0),
  .SHIFTIN3 (1'b0),
  .SHIFTIN4 (1'b0),
  .T1       (dqIO_w_en_0),
  .T2       (dqIO_w_en_0),
  .T3 (1'b0),
  .T4 (1'b0),
  .TCE      (1'b1),
  .TRAIN    (ioi_drp_train)
);

end
endgenerate

////////
//DQSP
////////

wire dqsp_oq ;
wire dqsp_tq ;

OSERDES2 #(
  .BYPASS_GCLK_FF ("TRUE"),
  .DATA_RATE_OQ  (C_OSERDES2_DATA_RATE_OQ),         // SDR, DDR      | Data Rate setting
  .DATA_RATE_OT  (C_OSERDES2_DATA_RATE_OT),         // SDR, DDR, BUF | Tristate Rate setting.
  .OUTPUT_MODE   (C_OSERDES2_OUTPUT_MODE_SE),          // SINGLE_ENDED, DIFFERENTIAL
  .SERDES_MODE   (C_OSERDES2_SERDES_MODE_MASTER),          // MASTER, SLAVE
  .DATA_WIDTH    (2)           // {1..8} 
) oserdes2_dqsp_0 
(
  .OQ       (dqsp_oq),
  .SHIFTOUT1(),
  .SHIFTOUT2(),
  .SHIFTOUT3(),
  .SHIFTOUT4(),
  .TQ       (dqsp_tq),
  .CLK0     (ioclk0),
  .CLK1(1'b0),
  .CLKDIV(1'b0),
  .D1       (1'b0),
  .D2       (1'b1),
  .D3(1'b0),
  .D4(1'b0),
  .IOCE     (pll_ce_0),
  .OCE      (1'b1),
  .RST      (int_sys_rst),
  .SHIFTIN1(1'b0),
  .SHIFTIN2(1'b0),
  .SHIFTIN3 (1'b0),
  .SHIFTIN4 (1'b0),
  .T1       (dqsIO_w_en_90_n),
  .T2       (dqsIO_w_en_90_p),
  .T3(1'b0),
  .T4(1'b0),
  .TCE      (1'b1),
  .TRAIN    (1'b0)
);

////////
//DQSN
////////

wire dqsn_oq ;
wire dqsn_tq ;



OSERDES2 #(
  .BYPASS_GCLK_FF ("TRUE"),
  .DATA_RATE_OQ  (C_OSERDES2_DATA_RATE_OQ),         // SDR, DDR      | Data Rate setting
  .DATA_RATE_OT  (C_OSERDES2_DATA_RATE_OT),         // SDR, DDR, BUF | Tristate Rate setting.
  .OUTPUT_MODE   (C_OSERDES2_OUTPUT_MODE_SE),          // SINGLE_ENDED, DIFFERENTIAL
  .SERDES_MODE   (C_OSERDES2_SERDES_MODE_SLAVE),          // MASTER, SLAVE
  .DATA_WIDTH    (2)           // {1..8} 
) oserdes2_dqsn_0 
(
  .OQ       (dqsn_oq),
  .SHIFTOUT1(),
  .SHIFTOUT2(),
  .SHIFTOUT3(),
  .SHIFTOUT4(),
  .TQ       (dqsn_tq),
  .CLK0     (ioclk0),
  .CLK1(1'b0),
  .CLKDIV(1'b0),
  .D1       (1'b1),
  .D2       (1'b0),
  .D3(1'b0),
  .D4(1'b0),
  .IOCE     (pll_ce_0),
  .OCE      (1'b1),
  .RST      (int_sys_rst),
  .SHIFTIN1 (1'b0),
  .SHIFTIN2 (1'b0),
  .SHIFTIN3(1'b0),
  .SHIFTIN4(1'b0),
  .T1       (dqsIO_w_en_90_n),
  .T2       (dqsIO_w_en_90_p),
  .T3(1'b0),
  .T4(1'b0),
  .TCE      (1'b1),
  .TRAIN    (1'b0)
);

////////
//UDQSP
////////

wire udqsp_oq ;
wire udqsp_tq ;


OSERDES2 #(
  .BYPASS_GCLK_FF ("TRUE"),
  .DATA_RATE_OQ  (C_OSERDES2_DATA_RATE_OQ),         // SDR, DDR      | Data Rate setting
  .DATA_RATE_OT  (C_OSERDES2_DATA_RATE_OT),         // SDR, DDR, BUF | Tristate Rate setting.
  .OUTPUT_MODE   (C_OSERDES2_OUTPUT_MODE_SE),          // SINGLE_ENDED, DIFFERENTIAL
  .SERDES_MODE   (C_OSERDES2_SERDES_MODE_MASTER),          // MASTER, SLAVE
  .DATA_WIDTH    (2)           // {1..8} 
) oserdes2_udqsp_0 
(
  .OQ       (udqsp_oq),
  .SHIFTOUT1(),
  .SHIFTOUT2(),
  .SHIFTOUT3(),
  .SHIFTOUT4(),
  .TQ       (udqsp_tq),
  .CLK0     (ioclk0),
  .CLK1(1'b0),
  .CLKDIV(1'b0),
  .D1       (1'b0),
  .D2       (1'b1),
  .D3(1'b0),
  .D4(1'b0),
  .IOCE     (pll_ce_0),
  .OCE      (1'b1),
  .RST      (int_sys_rst),
  .SHIFTIN1(1'b0),
  .SHIFTIN2(1'b0),
  .SHIFTIN3 (1'b0),
  .SHIFTIN4 (1'b0),
  .T1       (dqsIO_w_en_90_n),
  .T2       (dqsIO_w_en_90_p),
  .T3(1'b0),
  .T4(1'b0),
  .TCE      (1'b1),
  .TRAIN    (1'b0)
);

////////
//UDQSN
////////

wire udqsn_oq ;
wire udqsn_tq ;

OSERDES2 #(
  .BYPASS_GCLK_FF ("TRUE"),
  .DATA_RATE_OQ  (C_OSERDES2_DATA_RATE_OQ),         // SDR, DDR      | Data Rate setting
  .DATA_RATE_OT  (C_OSERDES2_DATA_RATE_OT),         // SDR, DDR, BUF | Tristate Rate setting.
  .OUTPUT_MODE   (C_OSERDES2_OUTPUT_MODE_SE),          // SINGLE_ENDED, DIFFERENTIAL
  .SERDES_MODE   (C_OSERDES2_SERDES_MODE_SLAVE),          // MASTER, SLAVE
  .DATA_WIDTH    (2)           // {1..8} 
) oserdes2_udqsn_0 
(
  .OQ       (udqsn_oq),
  .SHIFTOUT1(),
  .SHIFTOUT2(),
  .SHIFTOUT3(),
  .SHIFTOUT4(),
  .TQ       (udqsn_tq),
  .CLK0     (ioclk0),
  .CLK1(1'b0),
  .CLKDIV(1'b0),
  .D1       (1'b1),
  .D2       (1'b0),
  .D3(1'b0),
  .D4(1'b0),
  .IOCE     (pll_ce_0),
  .OCE      (1'b1),
  .RST      (int_sys_rst),
  .SHIFTIN1 (1'b0),
  .SHIFTIN2 (1'b0),
  .SHIFTIN3(1'b0),
  .SHIFTIN4(1'b0),
  .T1       (dqsIO_w_en_90_n),
  .T2       (dqsIO_w_en_90_p),
  .T3(1'b0),
  .T4(1'b0),
  .TCE      (1'b1),
  .TRAIN    (1'b0)
);

////////////////////////////////////////////////////////
//OSDDRES2 instantiations end
///////////////////////////////////////////////////////

wire aux_sdi_out_udqsp;
wire aux_sdi_out_10;
wire aux_sdi_out_11;
wire aux_sdi_out_12;
wire aux_sdi_out_14;
wire aux_sdi_out_15;

////////////////////////////////////////////////
//IODRP2 instantiations
////////////////////////////////////////////////
generate
if(C_NUM_DQ_PINS == 16 ) begin : dq_15_0_data
////////////////////////////////////////////////
//IODRP2 instantiations
////////////////////////////////////////////////

wire aux_sdi_out_14;
wire aux_sdi_out_15;
////////////////////////////////////////////////
//DQ14
////////////////////////////////////////////////
IODRP2_MCB #(
.DATA_RATE            (C_DQ_IODRP2_DATA_RATE),   // "SDR", "DDR"
.IDELAY_VALUE         (DQ14_TAP_DELAY_VAL),  // 0 to 255 inclusive
.MCB_ADDRESS          (7),  // 0 to 15
.ODELAY_VALUE         (0),  // 0 to 255 inclusive
.SERDES_MODE          (C_DQ_IODRP2_SERDES_MODE_MASTER),   // "NONE", "MASTER", "SLAVE"
.SIM_TAPDELAY_VALUE   (10)  // 10 to 90 inclusive
)
iodrp2_dq_14
(
  .AUXSDO             (aux_sdi_out_14),
  .DATAOUT(),
  .DATAOUT2(),
  .DOUT               (ioi_dq[14]),
  .DQSOUTN(),
  .DQSOUTP            (in_dq[14]),
  .SDO(),
  .TOUT               (t_dq[14]),
  .ADD                (ioi_drp_add),
  .AUXADDR            (ioi_drp_addr),
  .AUXSDOIN           (aux_sdi_out_15),
  .BKST               (ioi_drp_broadcast),
  .CLK                (ioi_drp_clk),
  .CS                 (ioi_drp_cs),
  .IDATAIN            (in_pre_dq[14]),
  .IOCLK0             (ioclk90),
  .IOCLK1(1'b0),
  .MEMUPDATE          (ioi_drp_update),
  .ODATAIN            (dq_oq[14]),
  .SDI                (ioi_drp_sdo),
  .T                  (dq_tq[14]) 
);


/////////////////////////////////////////////////
//DQ15
////////////////////////////////////////////////
IODRP2_MCB #(
.DATA_RATE            (C_DQ_IODRP2_DATA_RATE),   // "SDR", "DDR"
.IDELAY_VALUE         (DQ15_TAP_DELAY_VAL),  // 0 to 255 inclusive
.MCB_ADDRESS          (7),  // 0 to 15
.ODELAY_VALUE         (0),  // 0 to 255 inclusive
.SERDES_MODE          (C_DQ_IODRP2_SERDES_MODE_SLAVE),   // "NONE", "MASTER", "SLAVE"
.SIM_TAPDELAY_VALUE   (10)  // 10 to 90 inclusive

)
iodrp2_dq_15
(
  .AUXSDO             (aux_sdi_out_15),
  .DATAOUT(),
  .DATAOUT2(),
  .DOUT               (ioi_dq[15]),
  .DQSOUTN(),
  .DQSOUTP            (in_dq[15]),
  .SDO(),
  .TOUT               (t_dq[15]),
  .ADD                (ioi_drp_add),
  .AUXADDR            (ioi_drp_addr),
  .AUXSDOIN           (1'b0),
  .BKST               (ioi_drp_broadcast),
  .CLK                (ioi_drp_clk),
  .CS                 (ioi_drp_cs),
  .IDATAIN            (in_pre_dq[15]),
  .IOCLK0             (ioclk90),
  .IOCLK1(1'b0),
  .MEMUPDATE          (ioi_drp_update),
  .ODATAIN            (dq_oq[15]),
  .SDI                (ioi_drp_sdo),
  .T                  (dq_tq[15]) 
);



wire aux_sdi_out_12;
wire aux_sdi_out_13;
/////////////////////////////////////////////////
//DQ12
////////////////////////////////////////////////
IODRP2_MCB #(
.DATA_RATE            (C_DQ_IODRP2_DATA_RATE),   // "SDR", "DDR"
.IDELAY_VALUE         (DQ12_TAP_DELAY_VAL),  // 0 to 255 inclusive
.MCB_ADDRESS          (6),  // 0 to 15
.ODELAY_VALUE         (0),  // 0 to 255 inclusive
.SERDES_MODE          (C_DQ_IODRP2_SERDES_MODE_MASTER),   // "NONE", "MASTER", "SLAVE"
.SIM_TAPDELAY_VALUE   (10)  // 10 to 90 inclusive

)
iodrp2_dq_12
(
  .AUXSDO             (aux_sdi_out_12),
  .DATAOUT(),
  .DATAOUT2(),
  .DOUT               (ioi_dq[12]),
  .DQSOUTN(),
  .DQSOUTP            (in_dq[12]),
  .SDO(),
  .TOUT               (t_dq[12]),
  .ADD                (ioi_drp_add),
  .AUXADDR            (ioi_drp_addr),
  .AUXSDOIN           (aux_sdi_out_13),
  .BKST               (ioi_drp_broadcast),
  .CLK                (ioi_drp_clk),
  .CS                 (ioi_drp_cs),
  .IDATAIN            (in_pre_dq[12]),
  .IOCLK0             (ioclk90),
  .IOCLK1(1'b0),
  .MEMUPDATE          (ioi_drp_update),
  .ODATAIN            (dq_oq[12]),
  .SDI                (ioi_drp_sdo),
  .T                  (dq_tq[12]) 
);



/////////////////////////////////////////////////
//DQ13
////////////////////////////////////////////////
IODRP2_MCB #(
.DATA_RATE            (C_DQ_IODRP2_DATA_RATE),   // "SDR", "DDR"
.IDELAY_VALUE         (DQ13_TAP_DELAY_VAL),  // 0 to 255 inclusive
.MCB_ADDRESS          (6),  // 0 to 15
.ODELAY_VALUE         (0),  // 0 to 255 inclusive
.SERDES_MODE          (C_DQ_IODRP2_SERDES_MODE_SLAVE),   // "NONE", "MASTER", "SLAVE"
.SIM_TAPDELAY_VALUE   (10)  // 10 to 90 inclusive

)
iodrp2_dq_13
(
  .AUXSDO             (aux_sdi_out_13),
  .DATAOUT(),
  .DATAOUT2(),
  .DOUT               (ioi_dq[13]),
  .DQSOUTN(),
  .DQSOUTP            (in_dq[13]),
  .SDO(),
  .TOUT               (t_dq[13]),
  .ADD                (ioi_drp_add),
  .AUXADDR            (ioi_drp_addr),
  .AUXSDOIN           (aux_sdi_out_14),
  .BKST               (ioi_drp_broadcast),
  .CLK                (ioi_drp_clk),
  .CS                 (ioi_drp_cs),
  .IDATAIN            (in_pre_dq[13]),
  .IOCLK0             (ioclk90),
  .IOCLK1(1'b0),
  .MEMUPDATE          (ioi_drp_update),
  .ODATAIN            (dq_oq[13]),
  .SDI                (ioi_drp_sdo),
  .T                  (dq_tq[13]) 
);


wire aux_sdi_out_udqsp;
wire aux_sdi_out_udqsn;
/////////
//UDQSP
/////////
IODRP2_MCB #(
.DATA_RATE            (C_DQS_IODRP2_DATA_RATE),   // "SDR", "DDR"
.IDELAY_VALUE         (UDQSP_TAP_DELAY_VAL),  // 0 to 255 inclusive
.MCB_ADDRESS          (14),  // 0 to 15
.ODELAY_VALUE         (0),  // 0 to 255 inclusive
.SERDES_MODE          (C_DQS_IODRP2_SERDES_MODE_MASTER),   // "NONE", "MASTER", "SLAVE"
.SIM_TAPDELAY_VALUE   (10)  // 10 to 90 inclusive

)
iodrp2_udqsp_0
(
  .AUXSDO             (aux_sdi_out_udqsp),
  .DATAOUT(),
  .DATAOUT2(),
  .DOUT               (ioi_udqs),
  .DQSOUTN(),
  .DQSOUTP            (idelay_udqs_ioi_m),
  .SDO(),
  .TOUT               (t_udqs),
  .ADD                (ioi_drp_add),
  .AUXADDR            (ioi_drp_addr),
  .AUXSDOIN           (aux_sdi_out_udqsn),
  .BKST               (ioi_drp_broadcast),
  .CLK                (ioi_drp_clk),
  .CS                 (ioi_drp_cs),
  .IDATAIN            (in_pre_udqsp),
  .IOCLK0             (ioclk0),
  .IOCLK1(),
  .MEMUPDATE          (ioi_drp_update),
  .ODATAIN            (udqsp_oq),
  .SDI                (ioi_drp_sdo),
  .T                  (udqsp_tq) 
);

/////////
//UDQSN
/////////
IODRP2_MCB #(
.DATA_RATE            (C_DQS_IODRP2_DATA_RATE),   // "SDR", "DDR"
.IDELAY_VALUE         (UDQSN_TAP_DELAY_VAL),  // 0 to 255 inclusive
.MCB_ADDRESS          (14),  // 0 to 15
.ODELAY_VALUE         (0),  // 0 to 255 inclusive
.SERDES_MODE          (C_DQS_IODRP2_SERDES_MODE_SLAVE),   // "NONE", "MASTER", "SLAVE"
.SIM_TAPDELAY_VALUE   (10)  // 10 to 90 inclusive

)
iodrp2_udqsn_0
(
  .AUXSDO             (aux_sdi_out_udqsn),
  .DATAOUT(),
  .DATAOUT2(),
  .DOUT               (ioi_udqsn),
  .DQSOUTN(),
  .DQSOUTP            (idelay_udqs_ioi_s),
  .SDO(),
  .TOUT               (t_udqsn),
  .ADD                (ioi_drp_add),
  .AUXADDR            (ioi_drp_addr),
  .AUXSDOIN           (aux_sdi_out_12),
  .BKST               (ioi_drp_broadcast),
  .CLK                (ioi_drp_clk),
  .CS                 (ioi_drp_cs),
  .IDATAIN            (in_pre_udqsp),
  .IOCLK0             (ioclk0),
  .IOCLK1(),
  .MEMUPDATE          (ioi_drp_update),
  .ODATAIN            (udqsn_oq),
  .SDI                (ioi_drp_sdo),
  .T                  (udqsn_tq) 
);


wire aux_sdi_out_10;
wire aux_sdi_out_11;
/////////////////////////////////////////////////
//DQ10
////////////////////////////////////////////////
IODRP2_MCB #(
.DATA_RATE            (C_DQ_IODRP2_DATA_RATE),   // "SDR", "DDR"
.IDELAY_VALUE         (DQ10_TAP_DELAY_VAL),  // 0 to 255 inclusive
.MCB_ADDRESS          (5),  // 0 to 15
.ODELAY_VALUE         (0),  // 0 to 255 inclusive
.SERDES_MODE          (C_DQ_IODRP2_SERDES_MODE_MASTER),   // "NONE", "MASTER", "SLAVE"
.SIM_TAPDELAY_VALUE   (10)  // 10 to 90 inclusive

)
iodrp2_dq_10
(
  .AUXSDO             (aux_sdi_out_10),
  .DATAOUT(),
  .DATAOUT2(),
  .DOUT               (ioi_dq[10]),
  .DQSOUTN(),
  .DQSOUTP            (in_dq[10]),
  .SDO(),
  .TOUT               (t_dq[10]),
  .ADD                (ioi_drp_add),
  .AUXADDR            (ioi_drp_addr),
  .AUXSDOIN           (aux_sdi_out_11),
  .BKST               (ioi_drp_broadcast),
  .CLK                (ioi_drp_clk),
  .CS                 (ioi_drp_cs),
  .IDATAIN            (in_pre_dq[10]),
  .IOCLK0             (ioclk90),
  .IOCLK1(1'b0),
  .MEMUPDATE          (ioi_drp_update),
  .ODATAIN            (dq_oq[10]),
  .SDI                (ioi_drp_sdo),
  .T                  (dq_tq[10]) 
);


/////////////////////////////////////////////////
//DQ11
////////////////////////////////////////////////
IODRP2_MCB #(
.DATA_RATE            (C_DQ_IODRP2_DATA_RATE),   // "SDR", "DDR"
.IDELAY_VALUE         (DQ11_TAP_DELAY_VAL),  // 0 to 255 inclusive
.MCB_ADDRESS          (5),  // 0 to 15
.ODELAY_VALUE         (0),  // 0 to 255 inclusive
.SERDES_MODE          (C_DQ_IODRP2_SERDES_MODE_SLAVE),   // "NONE", "MASTER", "SLAVE"
.SIM_TAPDELAY_VALUE   (10)  // 10 to 90 inclusive

)
iodrp2_dq_11
(
  .AUXSDO             (aux_sdi_out_11),
  .DATAOUT(),
  .DATAOUT2(),
  .DOUT               (ioi_dq[11]),
  .DQSOUTN(),
  .DQSOUTP            (in_dq[11]),
  .SDO(),
  .TOUT               (t_dq[11]),
  .ADD                (ioi_drp_add),
  .AUXADDR            (ioi_drp_addr),
  .AUXSDOIN           (aux_sdi_out_udqsp),
  .BKST               (ioi_drp_broadcast),
  .CLK                (ioi_drp_clk),
  .CS                 (ioi_drp_cs),
  .IDATAIN            (in_pre_dq[11]),
  .IOCLK0             (ioclk90),
  .IOCLK1(1'b0),
  .MEMUPDATE          (ioi_drp_update),
  .ODATAIN            (dq_oq[11]),
  .SDI                (ioi_drp_sdo),
  .T                  (dq_tq[11])
);



wire aux_sdi_out_8;
wire aux_sdi_out_9;
/////////////////////////////////////////////////
//DQ8
////////////////////////////////////////////////
IODRP2_MCB #(
.DATA_RATE            (C_DQ_IODRP2_DATA_RATE),   // "SDR", "DDR"
.IDELAY_VALUE         (DQ8_TAP_DELAY_VAL),  // 0 to 255 inclusive
.MCB_ADDRESS          (4),  // 0 to 15
.ODELAY_VALUE         (0),  // 0 to 255 inclusive
.SERDES_MODE          (C_DQ_IODRP2_SERDES_MODE_MASTER),   // "NONE", "MASTER", "SLAVE"
.SIM_TAPDELAY_VALUE   (10)  // 10 to 90 inclusive

)
iodrp2_dq_8
(
  .AUXSDO             (aux_sdi_out_8),
  .DATAOUT(),
  .DATAOUT2(),
  .DOUT               (ioi_dq[8]),
  .DQSOUTN(),
  .DQSOUTP            (in_dq[8]),
  .SDO(),
  .TOUT               (t_dq[8]),
  .ADD                (ioi_drp_add),
  .AUXADDR            (ioi_drp_addr),
  .AUXSDOIN           (aux_sdi_out_9),
  .BKST               (ioi_drp_broadcast),
  .CLK                (ioi_drp_clk),
  .CS                 (ioi_drp_cs),
  .IDATAIN            (in_pre_dq[8]),
  .IOCLK0             (ioclk90),
  .IOCLK1(1'b0),
  .MEMUPDATE          (ioi_drp_update),
  .ODATAIN            (dq_oq[8]),
  .SDI                (ioi_drp_sdo),
  .T                  (dq_tq[8]) 
);


/////////////////////////////////////////////////
//DQ9
////////////////////////////////////////////////
IODRP2_MCB #(
.DATA_RATE            (C_DQ_IODRP2_DATA_RATE),   // "SDR", "DDR"
.IDELAY_VALUE         (DQ9_TAP_DELAY_VAL),  // 0 to 255 inclusive
.MCB_ADDRESS          (4),  // 0 to 15
.ODELAY_VALUE         (0),  // 0 to 255 inclusive
.SERDES_MODE          (C_DQ_IODRP2_SERDES_MODE_SLAVE),   // "NONE", "MASTER", "SLAVE"
.SIM_TAPDELAY_VALUE   (10)  // 10 to 90 inclusive

)
iodrp2_dq_9
(
  .AUXSDO             (aux_sdi_out_9),
  .DATAOUT(),
  .DATAOUT2(),
  .DOUT               (ioi_dq[9]),
  .DQSOUTN(),
  .DQSOUTP            (in_dq[9]),
  .SDO(),
  .TOUT               (t_dq[9]),
  .ADD                (ioi_drp_add),
  .AUXADDR            (ioi_drp_addr),
  .AUXSDOIN           (aux_sdi_out_10),
  .BKST               (ioi_drp_broadcast),
  .CLK                (ioi_drp_clk),
  .CS                 (ioi_drp_cs),
  .IDATAIN            (in_pre_dq[9]),
  .IOCLK0             (ioclk90),
  .IOCLK1(1'b0),
  .MEMUPDATE          (ioi_drp_update),
  .ODATAIN            (dq_oq[9]),
  .SDI                (ioi_drp_sdo),
  .T                  (dq_tq[9]) 
);


wire aux_sdi_out_0;
wire aux_sdi_out_1;
/////////////////////////////////////////////////
//DQ0
////////////////////////////////////////////////
IODRP2_MCB #(
.DATA_RATE            (C_DQ_IODRP2_DATA_RATE),   // "SDR", "DDR"
.IDELAY_VALUE         (DQ0_TAP_DELAY_VAL),  // 0 to 255 inclusive
.MCB_ADDRESS          (0),  // 0 to 15
.ODELAY_VALUE         (0),  // 0 to 255 inclusive
.SERDES_MODE          (C_DQ_IODRP2_SERDES_MODE_MASTER),   // "NONE", "MASTER", "SLAVE"
.SIM_TAPDELAY_VALUE   (10)  // 10 to 90 inclusive

)
iodrp2_dq_0
(
  .AUXSDO             (aux_sdi_out_0),
  .DATAOUT(),
  .DATAOUT2(),
  .DOUT               (ioi_dq[0]),
  .DQSOUTN(),
  .DQSOUTP            (in_dq[0]),
  .SDO(),
  .TOUT               (t_dq[0]),
  .ADD                (ioi_drp_add),
  .AUXADDR            (ioi_drp_addr),
  .AUXSDOIN           (aux_sdi_out_1),
  .BKST               (ioi_drp_broadcast),
  .CLK                (ioi_drp_clk),
  .CS                 (ioi_drp_cs),
  .IDATAIN            (in_pre_dq[0]),
  .IOCLK0             (ioclk90),
  .IOCLK1(1'b0),
  .MEMUPDATE          (ioi_drp_update),
  .ODATAIN            (dq_oq[0]),
  .SDI                (ioi_drp_sdo),
  .T                  (dq_tq[0]) 
);


/////////////////////////////////////////////////
//DQ1
////////////////////////////////////////////////
IODRP2_MCB #(
.DATA_RATE            (C_DQ_IODRP2_DATA_RATE),   // "SDR", "DDR"
.IDELAY_VALUE         (DQ1_TAP_DELAY_VAL),  // 0 to 255 inclusive
.MCB_ADDRESS          (0),  // 0 to 15
.ODELAY_VALUE         (0),  // 0 to 255 inclusive
.SERDES_MODE          (C_DQ_IODRP2_SERDES_MODE_SLAVE),   // "NONE", "MASTER", "SLAVE"
.SIM_TAPDELAY_VALUE   (10)  // 10 to 90 inclusive

)
iodrp2_dq_1
(
  .AUXSDO             (aux_sdi_out_1),
  .DATAOUT(),
  .DATAOUT2(),
  .DOUT               (ioi_dq[1]),
  .DQSOUTN(),
  .DQSOUTP            (in_dq[1]),
  .SDO(),
  .TOUT               (t_dq[1]),
  .ADD                (ioi_drp_add),
  .AUXADDR            (ioi_drp_addr),
  .AUXSDOIN           (aux_sdi_out_8),
  .BKST               (ioi_drp_broadcast),
  .CLK                (ioi_drp_clk),
  .CS                 (ioi_drp_cs),
  .IDATAIN            (in_pre_dq[1]),
  .IOCLK0             (ioclk90),
  .IOCLK1(),
  .MEMUPDATE          (ioi_drp_update),
  .ODATAIN            (dq_oq[1]),
  .SDI                (ioi_drp_sdo),
  .T                  (dq_tq[1]) 
);


wire aux_sdi_out_2;
wire aux_sdi_out_3;
/////////////////////////////////////////////////
//DQ2
////////////////////////////////////////////////
IODRP2_MCB #(
.DATA_RATE            (C_DQ_IODRP2_DATA_RATE),   // "SDR", "DDR"
.IDELAY_VALUE         (DQ2_TAP_DELAY_VAL),  // 0 to 255 inclusive
.MCB_ADDRESS          (1),  // 0 to 15
.ODELAY_VALUE         (0),  // 0 to 255 inclusive
.SERDES_MODE          (C_DQ_IODRP2_SERDES_MODE_MASTER),   // "NONE", "MASTER", "SLAVE"
.SIM_TAPDELAY_VALUE   (10)  // 10 to 90 inclusive

)
iodrp2_dq_2
(
  .AUXSDO             (aux_sdi_out_2),
  .DATAOUT(),
  .DATAOUT2(),
  .DOUT               (ioi_dq[2]),
  .DQSOUTN(),
  .DQSOUTP            (in_dq[2]),
  .SDO(),
  .TOUT               (t_dq[2]),
  .ADD                (ioi_drp_add),
  .AUXADDR            (ioi_drp_addr),
  .AUXSDOIN           (aux_sdi_out_3),
  .BKST               (ioi_drp_broadcast),
  .CLK                (ioi_drp_clk),
  .CS                 (ioi_drp_cs),
  .IDATAIN            (in_pre_dq[2]),
  .IOCLK0             (ioclk90),
  .IOCLK1(1'b0),
  .MEMUPDATE          (ioi_drp_update),
  .ODATAIN            (dq_oq[2]),
  .SDI                (ioi_drp_sdo),
  .T                  (dq_tq[2]) 
);


/////////////////////////////////////////////////
//DQ3
////////////////////////////////////////////////
IODRP2_MCB #(
.DATA_RATE            (C_DQ_IODRP2_DATA_RATE),   // "SDR", "DDR"
.IDELAY_VALUE         (DQ3_TAP_DELAY_VAL),  // 0 to 255 inclusive
.MCB_ADDRESS          (1),  // 0 to 15
.ODELAY_VALUE         (0),  // 0 to 255 inclusive
.SERDES_MODE          (C_DQ_IODRP2_SERDES_MODE_SLAVE),   // "NONE", "MASTER", "SLAVE"
.SIM_TAPDELAY_VALUE   (10)  // 10 to 90 inclusive

)
iodrp2_dq_3
(
  .AUXSDO             (aux_sdi_out_3),
  .DATAOUT(),
  .DATAOUT2(),
  .DOUT               (ioi_dq[3]),
  .DQSOUTN(),
  .DQSOUTP            (in_dq[3]),
  .SDO(),
  .TOUT               (t_dq[3]),
  .ADD                (ioi_drp_add),
  .AUXADDR            (ioi_drp_addr),
  .AUXSDOIN           (aux_sdi_out_0),
  .BKST               (ioi_drp_broadcast),
  .CLK                (ioi_drp_clk),
  .CS                 (ioi_drp_cs),
  .IDATAIN            (in_pre_dq[3]),
  .IOCLK0             (ioclk90),
  .IOCLK1(1'b0),
  .MEMUPDATE          (ioi_drp_update),
  .ODATAIN            (dq_oq[3]),
  .SDI                (ioi_drp_sdo),
  .T                  (dq_tq[3]) 
);


wire aux_sdi_out_dqsp;
wire aux_sdi_out_dqsn;
/////////
//DQSP
/////////
IODRP2_MCB #(
.DATA_RATE            (C_DQS_IODRP2_DATA_RATE),   // "SDR", "DDR"
.IDELAY_VALUE         (LDQSP_TAP_DELAY_VAL),  // 0 to 255 inclusive
.MCB_ADDRESS          (15),  // 0 to 15
.ODELAY_VALUE         (0),  // 0 to 255 inclusive
.SERDES_MODE          (C_DQS_IODRP2_SERDES_MODE_MASTER),   // "NONE", "MASTER", "SLAVE"
.SIM_TAPDELAY_VALUE   (10)  // 10 to 90 inclusive

)
iodrp2_dqsp_0
(
  .AUXSDO             (aux_sdi_out_dqsp),
  .DATAOUT(),
  .DATAOUT2(),
  .DOUT               (ioi_dqs),
  .DQSOUTN(),
  .DQSOUTP            (idelay_dqs_ioi_m),
  .SDO(),
  .TOUT               (t_dqs),
  .ADD                (ioi_drp_add),
  .AUXADDR            (ioi_drp_addr),
  .AUXSDOIN           (aux_sdi_out_dqsn),
  .BKST               (ioi_drp_broadcast),
  .CLK                (ioi_drp_clk),
  .CS                 (ioi_drp_cs),
  .IDATAIN            (in_pre_dqsp),
  .IOCLK0             (ioclk0),
  .IOCLK1(1'b0),
  .MEMUPDATE          (ioi_drp_update),
  .ODATAIN            (dqsp_oq),
  .SDI                (ioi_drp_sdo),
  .T                  (dqsp_tq) 
);

/////////
//DQSN
/////////
IODRP2_MCB #(
.DATA_RATE            (C_DQS_IODRP2_DATA_RATE),   // "SDR", "DDR"
.IDELAY_VALUE         (LDQSN_TAP_DELAY_VAL),  // 0 to 255 inclusive
.MCB_ADDRESS          (15),  // 0 to 15
.ODELAY_VALUE         (0),  // 0 to 255 inclusive
.SERDES_MODE          (C_DQS_IODRP2_SERDES_MODE_SLAVE),   // "NONE", "MASTER", "SLAVE"
.SIM_TAPDELAY_VALUE   (10)  // 10 to 90 inclusive

)
iodrp2_dqsn_0
(
  .AUXSDO             (aux_sdi_out_dqsn),
  .DATAOUT(),
  .DATAOUT2(),
  .DOUT               (ioi_dqsn),
  .DQSOUTN(),
  .DQSOUTP            (idelay_dqs_ioi_s),
  .SDO(),
  .TOUT               (t_dqsn),
  .ADD                (ioi_drp_add),
  .AUXADDR            (ioi_drp_addr),
  .AUXSDOIN           (aux_sdi_out_2),
  .BKST               (ioi_drp_broadcast),
  .CLK                (ioi_drp_clk),
  .CS                 (ioi_drp_cs),
  .IDATAIN            (in_pre_dqsp),
  .IOCLK0             (ioclk0),
  .IOCLK1(1'b0),
  .MEMUPDATE          (ioi_drp_update),
  .ODATAIN            (dqsn_oq),
  .SDI                (ioi_drp_sdo),
  .T                  (dqsn_tq) 
);

wire aux_sdi_out_6;
wire aux_sdi_out_7;
/////////////////////////////////////////////////
//DQ6
////////////////////////////////////////////////
IODRP2_MCB #(
.DATA_RATE            (C_DQ_IODRP2_DATA_RATE),   // "SDR", "DDR"
.IDELAY_VALUE         (DQ6_TAP_DELAY_VAL),  // 0 to 255 inclusive
.MCB_ADDRESS          (3),  // 0 to 15
.ODELAY_VALUE         (0),  // 0 to 255 inclusive
.SERDES_MODE          (C_DQ_IODRP2_SERDES_MODE_MASTER),   // "NONE", "MASTER", "SLAVE"
.SIM_TAPDELAY_VALUE   (10)  // 10 to 90 inclusive

)
iodrp2_dq_6
(
  .AUXSDO             (aux_sdi_out_6),
  .DATAOUT(),
  .DATAOUT2(),
  .DOUT               (ioi_dq[6]),
  .DQSOUTN(),
  .DQSOUTP            (in_dq[6]),
  .SDO(),
  .TOUT               (t_dq[6]),
  .ADD                (ioi_drp_add),
  .AUXADDR            (ioi_drp_addr),
  .AUXSDOIN           (aux_sdi_out_7),
  .BKST               (ioi_drp_broadcast),
  .CLK                (ioi_drp_clk),
  .CS                 (ioi_drp_cs),
  .IDATAIN            (in_pre_dq[6]),
  .IOCLK0             (ioclk90),
  .IOCLK1(1'b0),
  .MEMUPDATE          (ioi_drp_update),
  .ODATAIN            (dq_oq[6]),
  .SDI                (ioi_drp_sdo),
  .T                  (dq_tq[6]) 
);

/////////////////////////////////////////////////
//DQ7
////////////////////////////////////////////////
IODRP2_MCB #(
.DATA_RATE            (C_DQ_IODRP2_DATA_RATE),   // "SDR", "DDR"
.IDELAY_VALUE         (DQ7_TAP_DELAY_VAL),  // 0 to 255 inclusive
.MCB_ADDRESS          (3),  // 0 to 15
.ODELAY_VALUE         (0),  // 0 to 255 inclusive
.SERDES_MODE          (C_DQ_IODRP2_SERDES_MODE_SLAVE),   // "NONE", "MASTER", "SLAVE"
.SIM_TAPDELAY_VALUE   (10)  // 10 to 90 inclusive

)
iodrp2_dq_7
(
  .AUXSDO             (aux_sdi_out_7),
  .DATAOUT(),
  .DATAOUT2(),
  .DOUT               (ioi_dq[7]),
  .DQSOUTN(),
  .DQSOUTP            (in_dq[7]),
  .SDO(),
  .TOUT               (t_dq[7]),
  .ADD                (ioi_drp_add),
  .AUXADDR            (ioi_drp_addr),
  .AUXSDOIN           (aux_sdi_out_dqsp),
  .BKST               (ioi_drp_broadcast),
  .CLK                (ioi_drp_clk),
  .CS                 (ioi_drp_cs),
  .IDATAIN            (in_pre_dq[7]),
  .IOCLK0             (ioclk90),
  .IOCLK1(1'b0),
  .MEMUPDATE          (ioi_drp_update),
  .ODATAIN            (dq_oq[7]),
  .SDI                (ioi_drp_sdo),
  .T                  (dq_tq[7]) 
);



wire aux_sdi_out_4;
wire aux_sdi_out_5;
/////////////////////////////////////////////////
//DQ4
////////////////////////////////////////////////
IODRP2_MCB #(
.DATA_RATE            (C_DQ_IODRP2_DATA_RATE),   // "SDR", "DDR"
.IDELAY_VALUE         (DQ4_TAP_DELAY_VAL),  // 0 to 255 inclusive
.MCB_ADDRESS          (2),  // 0 to 15
.ODELAY_VALUE         (0),  // 0 to 255 inclusive
.SERDES_MODE          (C_DQ_IODRP2_SERDES_MODE_MASTER),   // "NONE", "MASTER", "SLAVE"
.SIM_TAPDELAY_VALUE   (10)  // 10 to 90 inclusive

)
iodrp2_dq_4
(
  .AUXSDO             (aux_sdi_out_4),
  .DATAOUT(),
  .DATAOUT2(),
  .DOUT               (ioi_dq[4]),
  .DQSOUTN(),
  .DQSOUTP            (in_dq[4]),
  .SDO(),
  .TOUT               (t_dq[4]),
  .ADD                (ioi_drp_add),
  .AUXADDR            (ioi_drp_addr),
  .AUXSDOIN           (aux_sdi_out_5),
  .BKST               (ioi_drp_broadcast),
  .CLK                (ioi_drp_clk),
  .CS                 (ioi_drp_cs),
  .IDATAIN            (in_pre_dq[4]),
  .IOCLK0             (ioclk90),
  .IOCLK1(1'b0),
  .MEMUPDATE          (ioi_drp_update),
  .ODATAIN            (dq_oq[4]),
  .SDI                (ioi_drp_sdo),
  .T                  (dq_tq[4]) 
);

/////////////////////////////////////////////////
//DQ5
////////////////////////////////////////////////
IODRP2_MCB #(
.DATA_RATE            (C_DQ_IODRP2_DATA_RATE),   // "SDR", "DDR"
.IDELAY_VALUE         (DQ5_TAP_DELAY_VAL),  // 0 to 255 inclusive
.MCB_ADDRESS          (2),  // 0 to 15
.ODELAY_VALUE         (0),  // 0 to 255 inclusive
.SERDES_MODE          (C_DQ_IODRP2_SERDES_MODE_SLAVE),   // "NONE", "MASTER", "SLAVE"
.SIM_TAPDELAY_VALUE   (10)  // 10 to 90 inclusive

)
iodrp2_dq_5
(
  .AUXSDO             (aux_sdi_out_5),
  .DATAOUT(),
  .DATAOUT2(),
  .DOUT               (ioi_dq[5]),
  .DQSOUTN(),
  .DQSOUTP            (in_dq[5]),
  .SDO(),
  .TOUT               (t_dq[5]),
  .ADD                (ioi_drp_add),
  .AUXADDR            (ioi_drp_addr),
  .AUXSDOIN           (aux_sdi_out_6),
  .BKST               (ioi_drp_broadcast),
  .CLK                (ioi_drp_clk),
  .CS                 (ioi_drp_cs),
  .IDATAIN            (in_pre_dq[5]),
  .IOCLK0             (ioclk90),
  .IOCLK1(1'b0),
  .MEMUPDATE          (ioi_drp_update),
  .ODATAIN            (dq_oq[5]),
  .SDI                (ioi_drp_sdo),
  .T                  (dq_tq[5]) 
);


//wire aux_sdi_out_udm;
wire aux_sdi_out_ldm;
/////////////////////////////////////////////////
//UDM
////////////////////////////////////////////////
IODRP2_MCB #(
.DATA_RATE            (C_DQ_IODRP2_DATA_RATE),   // "SDR", "DDR"
.IDELAY_VALUE         (0),  // 0 to 255 inclusive
.MCB_ADDRESS          (8),  // 0 to 15
.ODELAY_VALUE         (0),  // 0 to 255 inclusive
.SERDES_MODE          (C_DQ_IODRP2_SERDES_MODE_MASTER),   // "NONE", "MASTER", "SLAVE"
.SIM_TAPDELAY_VALUE   (10)  // 10 to 90 inclusive

)
iodrp2_dq_udm
(
  .AUXSDO             (ioi_drp_sdi),
  .DATAOUT(),
  .DATAOUT2(),
  .DOUT               (ioi_udm),
  .DQSOUTN(),
  .DQSOUTP(),
  .SDO(),
  .TOUT               (t_udm),
  .ADD                (ioi_drp_add),
  .AUXADDR            (ioi_drp_addr),
  .AUXSDOIN           (aux_sdi_out_ldm),
  .BKST               (ioi_drp_broadcast),
  .CLK                (ioi_drp_clk),
  .CS                 (ioi_drp_cs),
  .IDATAIN(1'b0),
  .IOCLK0             (ioclk90),
  .IOCLK1(1'b0),
  .MEMUPDATE          (ioi_drp_update),
  .ODATAIN            (udm_oq),
  .SDI                (ioi_drp_sdo),
  .T                  (udm_t) 
);


/////////////////////////////////////////////////
//LDM
////////////////////////////////////////////////
IODRP2_MCB #(
.DATA_RATE            (C_DQ_IODRP2_DATA_RATE),   // "SDR", "DDR"
.IDELAY_VALUE         (0),  // 0 to 255 inclusive
.MCB_ADDRESS          (8),  // 0 to 15
.ODELAY_VALUE         (0),  // 0 to 255 inclusive
.SERDES_MODE          (C_DQ_IODRP2_SERDES_MODE_SLAVE),   // "NONE", "MASTER", "SLAVE"
.SIM_TAPDELAY_VALUE   (10)  // 10 to 90 inclusive

)
iodrp2_dq_ldm
(
  .AUXSDO             (aux_sdi_out_ldm),
  .DATAOUT(),
  .DATAOUT2(),
  .DOUT               (ioi_ldm),
  .DQSOUTN(),
  .DQSOUTP(),
  .SDO(),
  .TOUT               (t_ldm),
  .ADD                (ioi_drp_add),
  .AUXADDR            (ioi_drp_addr),
  .AUXSDOIN           (aux_sdi_out_4),
  .BKST               (ioi_drp_broadcast),
  .CLK                (ioi_drp_clk),
  .CS                 (ioi_drp_cs),
  .IDATAIN(1'b0),
  .IOCLK0             (ioclk90),
  .IOCLK1(1'b0),
  .MEMUPDATE          (ioi_drp_update),
  .ODATAIN            (ldm_oq),
  .SDI                (ioi_drp_sdo),
  .T                  (ldm_t) 
);
end
endgenerate

generate
if(C_NUM_DQ_PINS == 8 ) begin : dq_7_0_data
wire aux_sdi_out_0;
wire aux_sdi_out_1;
/////////////////////////////////////////////////
//DQ0
////////////////////////////////////////////////
IODRP2_MCB #(
.DATA_RATE            (C_DQ_IODRP2_DATA_RATE),   // "SDR", "DDR"
.IDELAY_VALUE         (DQ0_TAP_DELAY_VAL),  // 0 to 255 inclusive
.MCB_ADDRESS          (0),  // 0 to 15
.ODELAY_VALUE         (0),  // 0 to 255 inclusive
.SERDES_MODE          (C_DQ_IODRP2_SERDES_MODE_MASTER),   // "NONE", "MASTER", "SLAVE"
.SIM_TAPDELAY_VALUE   (10)  // 10 to 90 inclusive

)
iodrp2_dq_0
(
  .AUXSDO             (aux_sdi_out_0),
  .DATAOUT(),
  .DATAOUT2(),
  .DOUT               (ioi_dq[0]),
  .DQSOUTN(),
  .DQSOUTP            (in_dq[0]),
  .SDO(),
  .TOUT               (t_dq[0]),
  .ADD                (ioi_drp_add),
  .AUXADDR            (ioi_drp_addr),
  .AUXSDOIN           (aux_sdi_out_1),
  .BKST               (ioi_drp_broadcast),
  .CLK                (ioi_drp_clk),
  .CS                 (ioi_drp_cs),
  .IDATAIN            (in_pre_dq[0]),
  .IOCLK0             (ioclk90),
  .IOCLK1(1'b0),
  .MEMUPDATE          (ioi_drp_update),
  .ODATAIN            (dq_oq[0]),
  .SDI                (ioi_drp_sdo),
  .T                  (dq_tq[0]) 
);


/////////////////////////////////////////////////
//DQ1
////////////////////////////////////////////////
IODRP2_MCB #(
.DATA_RATE            (C_DQ_IODRP2_DATA_RATE),   // "SDR", "DDR"
.IDELAY_VALUE         (DQ1_TAP_DELAY_VAL),  // 0 to 255 inclusive
.MCB_ADDRESS          (0),  // 0 to 15
.ODELAY_VALUE         (0),  // 0 to 255 inclusive
.SERDES_MODE          (C_DQ_IODRP2_SERDES_MODE_SLAVE),   // "NONE", "MASTER", "SLAVE"
.SIM_TAPDELAY_VALUE   (10)  // 10 to 90 inclusive

)
iodrp2_dq_1
(
  .AUXSDO             (aux_sdi_out_1),
  .DATAOUT(),
  .DATAOUT2(),
  .DOUT               (ioi_dq[1]),
  .DQSOUTN(),
  .DQSOUTP            (in_dq[1]),
  .SDO(),
  .TOUT               (t_dq[1]),
  .ADD                (ioi_drp_add),
  .AUXADDR            (ioi_drp_addr),
  .AUXSDOIN           (1'b0),
  .BKST               (ioi_drp_broadcast),
  .CLK                (ioi_drp_clk),
  .CS                 (ioi_drp_cs),
  .IDATAIN            (in_pre_dq[1]),
  .IOCLK0             (ioclk90),
  .IOCLK1(1'b0),
  .MEMUPDATE          (ioi_drp_update),
  .ODATAIN            (dq_oq[1]),
  .SDI                (ioi_drp_sdo),
  .T                  (dq_tq[1]) 
);


wire aux_sdi_out_2;
wire aux_sdi_out_3;
/////////////////////////////////////////////////
//DQ2
////////////////////////////////////////////////
IODRP2_MCB #(
.DATA_RATE            (C_DQ_IODRP2_DATA_RATE),   // "SDR", "DDR"
.IDELAY_VALUE         (DQ2_TAP_DELAY_VAL),  // 0 to 255 inclusive
.MCB_ADDRESS          (1),  // 0 to 15
.ODELAY_VALUE         (0),  // 0 to 255 inclusive
.SERDES_MODE          (C_DQ_IODRP2_SERDES_MODE_MASTER),   // "NONE", "MASTER", "SLAVE"
.SIM_TAPDELAY_VALUE   (10)  // 10 to 90 inclusive

)
iodrp2_dq_2
(
  .AUXSDO             (aux_sdi_out_2),
  .DATAOUT(),
  .DATAOUT2(),
  .DOUT               (ioi_dq[2]),
  .DQSOUTN(),
  .DQSOUTP            (in_dq[2]),
  .SDO(),
  .TOUT               (t_dq[2]),
  .ADD                (ioi_drp_add),
  .AUXADDR            (ioi_drp_addr),
  .AUXSDOIN           (aux_sdi_out_3),
  .BKST               (ioi_drp_broadcast),
  .CLK                (ioi_drp_clk),
  .CS                 (ioi_drp_cs),
  .IDATAIN            (in_pre_dq[2]),
  .IOCLK0             (ioclk90),
  .IOCLK1(1'b0),
  .MEMUPDATE          (ioi_drp_update),
  .ODATAIN            (dq_oq[2]),
  .SDI                (ioi_drp_sdo),
  .T                  (dq_tq[2]) 
);


/////////////////////////////////////////////////
//DQ3
////////////////////////////////////////////////
IODRP2_MCB #(
.DATA_RATE            (C_DQ_IODRP2_DATA_RATE),   // "SDR", "DDR"
.IDELAY_VALUE         (DQ3_TAP_DELAY_VAL),  // 0 to 255 inclusive
.MCB_ADDRESS          (1),  // 0 to 15
.ODELAY_VALUE         (0),  // 0 to 255 inclusive
.SERDES_MODE          (C_DQ_IODRP2_SERDES_MODE_SLAVE),   // "NONE", "MASTER", "SLAVE"
.SIM_TAPDELAY_VALUE   (10)  // 10 to 90 inclusive

)
iodrp2_dq_3
(
  .AUXSDO             (aux_sdi_out_3),
  .DATAOUT(),
  .DATAOUT2(),
  .DOUT               (ioi_dq[3]),
  .DQSOUTN(),
  .DQSOUTP            (in_dq[3]),
  .SDO(),
  .TOUT               (t_dq[3]),
  .ADD                (ioi_drp_add),
  .AUXADDR            (ioi_drp_addr),
  .AUXSDOIN           (aux_sdi_out_0),
  .BKST               (ioi_drp_broadcast),
  .CLK                (ioi_drp_clk),
  .CS                 (ioi_drp_cs),
  .IDATAIN            (in_pre_dq[3]),
  .IOCLK0             (ioclk90),
  .IOCLK1(1'b0),
  .MEMUPDATE          (ioi_drp_update),
  .ODATAIN            (dq_oq[3]),
  .SDI                (ioi_drp_sdo),
  .T                  (dq_tq[3]) 
);


wire aux_sdi_out_dqsp;
wire aux_sdi_out_dqsn;
/////////
//DQSP
/////////
IODRP2_MCB #(
.DATA_RATE            (C_DQS_IODRP2_DATA_RATE),   // "SDR", "DDR"
.IDELAY_VALUE         (LDQSP_TAP_DELAY_VAL),  // 0 to 255 inclusive
.MCB_ADDRESS          (15),  // 0 to 15
.ODELAY_VALUE         (0),  // 0 to 255 inclusive
.SERDES_MODE          (C_DQS_IODRP2_SERDES_MODE_MASTER),   // "NONE", "MASTER", "SLAVE"
.SIM_TAPDELAY_VALUE   (10)  // 10 to 90 inclusive

)
iodrp2_dqsp_0
(
  .AUXSDO             (aux_sdi_out_dqsp),
  .DATAOUT(),
  .DATAOUT2(),
  .DOUT               (ioi_dqs),
  .DQSOUTN(),
  .DQSOUTP            (idelay_dqs_ioi_m),
  .SDO(),
  .TOUT               (t_dqs),
  .ADD                (ioi_drp_add),
  .AUXADDR            (ioi_drp_addr),
  .AUXSDOIN           (aux_sdi_out_dqsn),
  .BKST               (ioi_drp_broadcast),
  .CLK                (ioi_drp_clk),
  .CS                 (ioi_drp_cs),
  .IDATAIN            (in_pre_dqsp),
  .IOCLK0             (ioclk0),
  .IOCLK1(1'b0),
  .MEMUPDATE          (ioi_drp_update),
  .ODATAIN            (dqsp_oq),
  .SDI                (ioi_drp_sdo),
  .T                  (dqsp_tq) 
);

/////////
//DQSN
/////////
IODRP2_MCB #(
.DATA_RATE            (C_DQS_IODRP2_DATA_RATE),   // "SDR", "DDR"
.IDELAY_VALUE         (LDQSN_TAP_DELAY_VAL),  // 0 to 255 inclusive
.MCB_ADDRESS          (15),  // 0 to 15
.ODELAY_VALUE         (0),  // 0 to 255 inclusive
.SERDES_MODE          (C_DQS_IODRP2_SERDES_MODE_SLAVE),   // "NONE", "MASTER", "SLAVE"
.SIM_TAPDELAY_VALUE   (10)  // 10 to 90 inclusive

)
iodrp2_dqsn_0
(
  .AUXSDO             (aux_sdi_out_dqsn),
  .DATAOUT(),
  .DATAOUT2(),
  .DOUT               (ioi_dqsn),
  .DQSOUTN(),
  .DQSOUTP            (idelay_dqs_ioi_s),
  .SDO(),
  .TOUT               (t_dqsn),
  .ADD                (ioi_drp_add),
  .AUXADDR            (ioi_drp_addr),
  .AUXSDOIN           (aux_sdi_out_2),
  .BKST               (ioi_drp_broadcast),
  .CLK                (ioi_drp_clk),
  .CS                 (ioi_drp_cs),
  .IDATAIN            (in_pre_dqsp),
  .IOCLK0             (ioclk0),
  .IOCLK1(1'b0),
  .MEMUPDATE          (ioi_drp_update),
  .ODATAIN            (dqsn_oq),
  .SDI                (ioi_drp_sdo),
  .T                  (dqsn_tq) 
);

wire aux_sdi_out_6;
wire aux_sdi_out_7;
/////////////////////////////////////////////////
//DQ6
////////////////////////////////////////////////
IODRP2_MCB #(
.DATA_RATE            (C_DQ_IODRP2_DATA_RATE),   // "SDR", "DDR"
.IDELAY_VALUE         (DQ6_TAP_DELAY_VAL),  // 0 to 255 inclusive
.MCB_ADDRESS          (3),  // 0 to 15
.ODELAY_VALUE         (0),  // 0 to 255 inclusive
.SERDES_MODE          (C_DQ_IODRP2_SERDES_MODE_MASTER),   // "NONE", "MASTER", "SLAVE"
.SIM_TAPDELAY_VALUE   (10)  // 10 to 90 inclusive

)
iodrp2_dq_6
(
  .AUXSDO             (aux_sdi_out_6),
  .DATAOUT(),
  .DATAOUT2(),
  .DOUT               (ioi_dq[6]),
  .DQSOUTN(),
  .DQSOUTP            (in_dq[6]),
  .SDO(),
  .TOUT               (t_dq[6]),
  .ADD                (ioi_drp_add),
  .AUXADDR            (ioi_drp_addr),
  .AUXSDOIN           (aux_sdi_out_7),
  .BKST               (ioi_drp_broadcast),
  .CLK                (ioi_drp_clk),
  .CS                 (ioi_drp_cs),
  .IDATAIN            (in_pre_dq[6]),
  .IOCLK0             (ioclk90),
  .IOCLK1(1'b0),
  .MEMUPDATE          (ioi_drp_update),
  .ODATAIN            (dq_oq[6]),
  .SDI                (ioi_drp_sdo),
  .T                  (dq_tq[6]) 
);

/////////////////////////////////////////////////
//DQ7
////////////////////////////////////////////////
IODRP2_MCB #(
.DATA_RATE            (C_DQ_IODRP2_DATA_RATE),   // "SDR", "DDR"
.IDELAY_VALUE         (DQ7_TAP_DELAY_VAL),  // 0 to 255 inclusive
.MCB_ADDRESS          (3),  // 0 to 15
.ODELAY_VALUE         (0),  // 0 to 255 inclusive
.SERDES_MODE          (C_DQ_IODRP2_SERDES_MODE_SLAVE),   // "NONE", "MASTER", "SLAVE"
.SIM_TAPDELAY_VALUE   (10)  // 10 to 90 inclusive

)
iodrp2_dq_7
(
  .AUXSDO             (aux_sdi_out_7),
  .DATAOUT(),
  .DATAOUT2(),
  .DOUT               (ioi_dq[7]),
  .DQSOUTN(),
  .DQSOUTP            (in_dq[7]),
  .SDO(),
  .TOUT               (t_dq[7]),
  .ADD                (ioi_drp_add),
  .AUXADDR            (ioi_drp_addr),
  .AUXSDOIN           (aux_sdi_out_dqsp),
  .BKST               (ioi_drp_broadcast),
  .CLK                (ioi_drp_clk),
  .CS                 (ioi_drp_cs),
  .IDATAIN            (in_pre_dq[7]),
  .IOCLK0             (ioclk90),
  .IOCLK1(1'b0),
  .MEMUPDATE          (ioi_drp_update),
  .ODATAIN            (dq_oq[7]),
  .SDI                (ioi_drp_sdo),
  .T                  (dq_tq[7]) 
);



wire aux_sdi_out_4;
wire aux_sdi_out_5;
/////////////////////////////////////////////////
//DQ4
////////////////////////////////////////////////
IODRP2_MCB #(
.DATA_RATE            (C_DQ_IODRP2_DATA_RATE),   // "SDR", "DDR"
.IDELAY_VALUE         (DQ4_TAP_DELAY_VAL),  // 0 to 255 inclusive
.MCB_ADDRESS          (2),  // 0 to 15
.ODELAY_VALUE         (0),  // 0 to 255 inclusive
.SERDES_MODE          (C_DQ_IODRP2_SERDES_MODE_MASTER),   // "NONE", "MASTER", "SLAVE"
.SIM_TAPDELAY_VALUE   (10)  // 10 to 90 inclusive

)
iodrp2_dq_4
(
  .AUXSDO             (aux_sdi_out_4),
  .DATAOUT(),
  .DATAOUT2(),
  .DOUT               (ioi_dq[4]),
  .DQSOUTN(),
  .DQSOUTP            (in_dq[4]),
  .SDO(),
  .TOUT               (t_dq[4]),
  .ADD                (ioi_drp_add),
  .AUXADDR            (ioi_drp_addr),
  .AUXSDOIN           (aux_sdi_out_5),
  .BKST               (ioi_drp_broadcast),
  .CLK                (ioi_drp_clk),
  .CS                 (ioi_drp_cs),
  .IDATAIN            (in_pre_dq[4]),
  .IOCLK0             (ioclk90),
  .IOCLK1(1'b0),
  .MEMUPDATE          (ioi_drp_update),
  .ODATAIN            (dq_oq[4]),
  .SDI                (ioi_drp_sdo),
  .T                  (dq_tq[4]) 
);

/////////////////////////////////////////////////
//DQ5
////////////////////////////////////////////////
IODRP2_MCB #(
.DATA_RATE            (C_DQ_IODRP2_DATA_RATE),   // "SDR", "DDR"
.IDELAY_VALUE         (DQ5_TAP_DELAY_VAL),  // 0 to 255 inclusive
.MCB_ADDRESS          (2),  // 0 to 15
.ODELAY_VALUE         (0),  // 0 to 255 inclusive
.SERDES_MODE          (C_DQ_IODRP2_SERDES_MODE_SLAVE),   // "NONE", "MASTER", "SLAVE"
.SIM_TAPDELAY_VALUE   (10)  // 10 to 90 inclusive

)
iodrp2_dq_5
(
  .AUXSDO             (aux_sdi_out_5),
  .DATAOUT(),
  .DATAOUT2(),
  .DOUT               (ioi_dq[5]),
  .DQSOUTN(),
  .DQSOUTP            (in_dq[5]),
  .SDO(),
  .TOUT               (t_dq[5]),
  .ADD                (ioi_drp_add),
  .AUXADDR            (ioi_drp_addr),
  .AUXSDOIN           (aux_sdi_out_6),
  .BKST               (ioi_drp_broadcast),
  .CLK                (ioi_drp_clk),
  .CS                 (ioi_drp_cs),
  .IDATAIN            (in_pre_dq[5]),
  .IOCLK0             (ioclk90),
  .IOCLK1(1'b0),
  .MEMUPDATE          (ioi_drp_update),
  .ODATAIN            (dq_oq[5]),
  .SDI                (ioi_drp_sdo),
  .T                  (dq_tq[5]) 
);

//NEED TO GENERATE UDM so that user won't instantiate in this location
//wire aux_sdi_out_udm;
wire aux_sdi_out_ldm;
/////////////////////////////////////////////////
//UDM
////////////////////////////////////////////////
IODRP2_MCB #(
.DATA_RATE            (C_DQ_IODRP2_DATA_RATE),   // "SDR", "DDR"
.IDELAY_VALUE         (0),  // 0 to 255 inclusive
.MCB_ADDRESS          (8),  // 0 to 15
.ODELAY_VALUE         (0),  // 0 to 255 inclusive
.SERDES_MODE          (C_DQ_IODRP2_SERDES_MODE_MASTER),   // "NONE", "MASTER", "SLAVE"
.SIM_TAPDELAY_VALUE   (10)  // 10 to 90 inclusive

)
iodrp2_dq_udm
(
  .AUXSDO             (ioi_drp_sdi),
  .DATAOUT(),
  .DATAOUT2(),
  .DOUT               (ioi_udm),
  .DQSOUTN(),
  .DQSOUTP(),
  .SDO(),
  .TOUT               (t_udm),
  .ADD                (ioi_drp_add),
  .AUXADDR            (ioi_drp_addr),
  .AUXSDOIN           (aux_sdi_out_ldm),
  .BKST               (ioi_drp_broadcast),
  .CLK                (ioi_drp_clk),
  .CS                 (ioi_drp_cs),
  .IDATAIN(1'b0),
  .IOCLK0             (ioclk90),
  .IOCLK1(1'b0),
  .MEMUPDATE          (ioi_drp_update),
  .ODATAIN            (udm_oq),
  .SDI                (ioi_drp_sdo),
  .T                  (udm_t) 
);


/////////////////////////////////////////////////
//LDM
////////////////////////////////////////////////
IODRP2_MCB #(
.DATA_RATE            (C_DQ_IODRP2_DATA_RATE),   // "SDR", "DDR"
.IDELAY_VALUE         (0),  // 0 to 255 inclusive
.MCB_ADDRESS          (8),  // 0 to 15
.ODELAY_VALUE         (0),  // 0 to 255 inclusive
.SERDES_MODE          (C_DQ_IODRP2_SERDES_MODE_SLAVE),   // "NONE", "MASTER", "SLAVE"
.SIM_TAPDELAY_VALUE   (10)  // 10 to 90 inclusive

)
iodrp2_dq_ldm
(
  .AUXSDO             (aux_sdi_out_ldm),
  .DATAOUT(),
  .DATAOUT2(),
  .DOUT               (ioi_ldm),
  .DQSOUTN(),
  .DQSOUTP(),
  .SDO(),
  .TOUT               (t_ldm),
  .ADD                (ioi_drp_add),
  .AUXADDR            (ioi_drp_addr),
  .AUXSDOIN           (aux_sdi_out_4),
  .BKST               (ioi_drp_broadcast),
  .CLK                (ioi_drp_clk),
  .CS                 (ioi_drp_cs),
  .IDATAIN(1'b0),
  .IOCLK0             (ioclk90),
  .IOCLK1(1'b0),
  .MEMUPDATE          (ioi_drp_update),
  .ODATAIN            (ldm_oq),
  .SDI                (ioi_drp_sdo),
  .T                  (ldm_t) 
);
end
endgenerate

generate
if(C_NUM_DQ_PINS == 4 ) begin : dq_3_0_data

wire aux_sdi_out_0;
wire aux_sdi_out_1;
/////////////////////////////////////////////////
//DQ0
////////////////////////////////////////////////
IODRP2_MCB #(
.DATA_RATE            (C_DQ_IODRP2_DATA_RATE),   // "SDR", "DDR"
.IDELAY_VALUE         (DQ0_TAP_DELAY_VAL),  // 0 to 255 inclusive
.MCB_ADDRESS          (0),  // 0 to 15
.ODELAY_VALUE         (0),  // 0 to 255 inclusive
.SERDES_MODE          (C_DQ_IODRP2_SERDES_MODE_MASTER),   // "NONE", "MASTER", "SLAVE"
.SIM_TAPDELAY_VALUE   (10)  // 10 to 90 inclusive

)
iodrp2_dq_0
(
  .AUXSDO             (aux_sdi_out_0),
  .DATAOUT(),
  .DATAOUT2(),
  .DOUT               (ioi_dq[0]),
  .DQSOUTN(),
  .DQSOUTP            (in_dq[0]),
  .SDO(),
  .TOUT               (t_dq[0]),
  .ADD                (ioi_drp_add),
  .AUXADDR            (ioi_drp_addr),
  .AUXSDOIN           (aux_sdi_out_1),
  .BKST               (ioi_drp_broadcast),
  .CLK                (ioi_drp_clk),
  .CS                 (ioi_drp_cs),
  .IDATAIN            (in_pre_dq[0]),
  .IOCLK0             (ioclk90),
  .IOCLK1(1'b0),
  .MEMUPDATE          (ioi_drp_update),
  .ODATAIN            (dq_oq[0]),
  .SDI                (ioi_drp_sdo),
  .T                  (dq_tq[0]) 
);


/////////////////////////////////////////////////
//DQ1
////////////////////////////////////////////////
IODRP2_MCB #(
.DATA_RATE            (C_DQ_IODRP2_DATA_RATE),   // "SDR", "DDR"
.IDELAY_VALUE         (DQ1_TAP_DELAY_VAL),  // 0 to 255 inclusive
.MCB_ADDRESS          (0),  // 0 to 15
.ODELAY_VALUE         (0),  // 0 to 255 inclusive
.SERDES_MODE          (C_DQ_IODRP2_SERDES_MODE_SLAVE),   // "NONE", "MASTER", "SLAVE"
.SIM_TAPDELAY_VALUE   (10)  // 10 to 90 inclusive

)
iodrp2_dq_1
(
  .AUXSDO             (aux_sdi_out_1),
  .DATAOUT(),
  .DATAOUT2(),
  .DOUT               (ioi_dq[1]),
  .DQSOUTN(),
  .DQSOUTP            (in_dq[1]),
  .SDO(),
  .TOUT               (t_dq[1]),
  .ADD                (ioi_drp_add),
  .AUXADDR            (ioi_drp_addr),
  .AUXSDOIN           (1'b0),
  .BKST               (ioi_drp_broadcast),
  .CLK                (ioi_drp_clk),
  .CS                 (ioi_drp_cs),
  .IDATAIN            (in_pre_dq[1]),
  .IOCLK0             (ioclk90),
  .IOCLK1(1'b0),
  .MEMUPDATE          (ioi_drp_update),
  .ODATAIN            (dq_oq[1]),
  .SDI                (ioi_drp_sdo),
  .T                  (dq_tq[1]) 
);


wire aux_sdi_out_2;
wire aux_sdi_out_3;
/////////////////////////////////////////////////
//DQ2
////////////////////////////////////////////////
IODRP2_MCB #(
.DATA_RATE            (C_DQ_IODRP2_DATA_RATE),   // "SDR", "DDR"
.IDELAY_VALUE         (DQ2_TAP_DELAY_VAL),  // 0 to 255 inclusive
.MCB_ADDRESS          (1),  // 0 to 15
.ODELAY_VALUE         (0),  // 0 to 255 inclusive
.SERDES_MODE          (C_DQ_IODRP2_SERDES_MODE_MASTER),   // "NONE", "MASTER", "SLAVE"
.SIM_TAPDELAY_VALUE   (10)  // 10 to 90 inclusive

)
iodrp2_dq_2
(
  .AUXSDO             (aux_sdi_out_2),
  .DATAOUT(),
  .DATAOUT2(),
  .DOUT               (ioi_dq[2]),
  .DQSOUTN(),
  .DQSOUTP            (in_dq[2]),
  .SDO(),
  .TOUT               (t_dq[2]),
  .ADD                (ioi_drp_add),
  .AUXADDR            (ioi_drp_addr),
  .AUXSDOIN           (aux_sdi_out_3),
  .BKST               (ioi_drp_broadcast),
  .CLK                (ioi_drp_clk),
  .CS                 (ioi_drp_cs),
  .IDATAIN            (in_pre_dq[2]),
  .IOCLK0             (ioclk90),
  .IOCLK1(1'b0),
  .MEMUPDATE          (ioi_drp_update),
  .ODATAIN            (dq_oq[2]),
  .SDI                (ioi_drp_sdo),
  .T                  (dq_tq[2]) 
);


/////////////////////////////////////////////////
//DQ3
////////////////////////////////////////////////
IODRP2_MCB #(
.DATA_RATE            (C_DQ_IODRP2_DATA_RATE),   // "SDR", "DDR"
.IDELAY_VALUE         (DQ3_TAP_DELAY_VAL),  // 0 to 255 inclusive
.MCB_ADDRESS          (1),  // 0 to 15
.ODELAY_VALUE         (0),  // 0 to 255 inclusive
.SERDES_MODE          (C_DQ_IODRP2_SERDES_MODE_SLAVE),   // "NONE", "MASTER", "SLAVE"
.SIM_TAPDELAY_VALUE   (10)  // 10 to 90 inclusive

)
iodrp2_dq_3
(
  .AUXSDO             (aux_sdi_out_3),
  .DATAOUT(),
  .DATAOUT2(),
  .DOUT               (ioi_dq[3]),
  .DQSOUTN(),
  .DQSOUTP            (in_dq[3]),
  .SDO(),
  .TOUT               (t_dq[3]),
  .ADD                (ioi_drp_add),
  .AUXADDR            (ioi_drp_addr),
  .AUXSDOIN           (aux_sdi_out_0),
  .BKST               (ioi_drp_broadcast),
  .CLK                (ioi_drp_clk),
  .CS                 (ioi_drp_cs),
  .IDATAIN            (in_pre_dq[3]),
  .IOCLK0             (ioclk90),
  .IOCLK1(1'b0),
  .MEMUPDATE          (ioi_drp_update),
  .ODATAIN            (dq_oq[3]),
  .SDI                (ioi_drp_sdo),
  .T                  (dq_tq[3]) 
);


wire aux_sdi_out_dqsp;
wire aux_sdi_out_dqsn;
/////////
//DQSP
/////////
IODRP2_MCB #(
.DATA_RATE            (C_DQS_IODRP2_DATA_RATE),   // "SDR", "DDR"
.IDELAY_VALUE         (LDQSP_TAP_DELAY_VAL),  // 0 to 255 inclusive
.MCB_ADDRESS          (15),  // 0 to 15
.ODELAY_VALUE         (0),  // 0 to 255 inclusive
.SERDES_MODE          (C_DQS_IODRP2_SERDES_MODE_MASTER),   // "NONE", "MASTER", "SLAVE"
.SIM_TAPDELAY_VALUE   (10)  // 10 to 90 inclusive

)
iodrp2_dqsp_0
(
  .AUXSDO             (aux_sdi_out_dqsp),
  .DATAOUT(),
  .DATAOUT2(),
  .DOUT               (ioi_dqs),
  .DQSOUTN(),
  .DQSOUTP            (idelay_dqs_ioi_m),
  .SDO(),
  .TOUT               (t_dqs),
  .ADD                (ioi_drp_add),
  .AUXADDR            (ioi_drp_addr),
  .AUXSDOIN           (aux_sdi_out_dqsn),
  .BKST               (ioi_drp_broadcast),
  .CLK                (ioi_drp_clk),
  .CS                 (ioi_drp_cs),
  .IDATAIN            (in_pre_dqsp),
  .IOCLK0             (ioclk0),
  .IOCLK1(1'b0),
  .MEMUPDATE          (ioi_drp_update),
  .ODATAIN            (dqsp_oq),
  .SDI                (ioi_drp_sdo),
  .T                  (dqsp_tq) 
);

/////////
//DQSN
/////////
IODRP2_MCB #(
.DATA_RATE            (C_DQS_IODRP2_DATA_RATE),   // "SDR", "DDR"
.IDELAY_VALUE         (LDQSN_TAP_DELAY_VAL),  // 0 to 255 inclusive
.MCB_ADDRESS          (15),  // 0 to 15
.ODELAY_VALUE         (0),  // 0 to 255 inclusive
.SERDES_MODE          (C_DQS_IODRP2_SERDES_MODE_SLAVE),   // "NONE", "MASTER", "SLAVE"
.SIM_TAPDELAY_VALUE   (10)  // 10 to 90 inclusive

)
iodrp2_dqsn_0
(
  .AUXSDO             (aux_sdi_out_dqsn),
  .DATAOUT(),
  .DATAOUT2(),
  .DOUT               (ioi_dqsn),
  .DQSOUTN(),
  .DQSOUTP            (idelay_dqs_ioi_s),
  .SDO(),
  .TOUT               (t_dqsn),
  .ADD                (ioi_drp_add),
  .AUXADDR            (ioi_drp_addr),
  .AUXSDOIN           (aux_sdi_out_2),
  .BKST               (ioi_drp_broadcast),
  .CLK                (ioi_drp_clk),
  .CS                 (ioi_drp_cs),
  .IDATAIN            (in_pre_dqsp),
  .IOCLK0             (ioclk0),
  .IOCLK1(1'b0),
  .MEMUPDATE          (ioi_drp_update),
  .ODATAIN            (dqsn_oq),
  .SDI                (ioi_drp_sdo),
  .T                  (dqsn_tq) 
);

//NEED TO GENERATE UDM so that user won't instantiate in this location
//wire aux_sdi_out_udm;
wire aux_sdi_out_ldm;
/////////////////////////////////////////////////
//UDM
////////////////////////////////////////////////
IODRP2_MCB #(
.DATA_RATE            (C_DQ_IODRP2_DATA_RATE),   // "SDR", "DDR"
.IDELAY_VALUE         (0),  // 0 to 255 inclusive
.MCB_ADDRESS          (8),  // 0 to 15
.ODELAY_VALUE         (0),  // 0 to 255 inclusive
.SERDES_MODE          (C_DQ_IODRP2_SERDES_MODE_MASTER),   // "NONE", "MASTER", "SLAVE"
.SIM_TAPDELAY_VALUE   (10)  // 10 to 90 inclusive

)
iodrp2_dq_udm
(
  .AUXSDO             (ioi_drp_sdi),
  .DATAOUT(),
  .DATAOUT2(),
  .DOUT               (ioi_udm),
  .DQSOUTN(),
  .DQSOUTP(),
  .SDO(),
  .TOUT               (t_udm),
  .ADD                (ioi_drp_add),
  .AUXADDR            (ioi_drp_addr),
  .AUXSDOIN           (aux_sdi_out_ldm),
  .BKST               (ioi_drp_broadcast),
  .CLK                (ioi_drp_clk),
  .CS                 (ioi_drp_cs),
  .IDATAIN(1'b0),
  .IOCLK0             (ioclk90),
  .IOCLK1(1'b0),
  .MEMUPDATE          (ioi_drp_update),
  .ODATAIN            (udm_oq),
  .SDI                (ioi_drp_sdo),
  .T                  (udm_t) 
);


/////////////////////////////////////////////////
//LDM
////////////////////////////////////////////////
IODRP2_MCB #(
.DATA_RATE            (C_DQ_IODRP2_DATA_RATE),   // "SDR", "DDR"
.IDELAY_VALUE         (0),  // 0 to 255 inclusive
.MCB_ADDRESS          (8),  // 0 to 15
.ODELAY_VALUE         (0),  // 0 to 255 inclusive
.SERDES_MODE          (C_DQ_IODRP2_SERDES_MODE_SLAVE),   // "NONE", "MASTER", "SLAVE"
.SIM_TAPDELAY_VALUE   (10)  // 10 to 90 inclusive

)
iodrp2_dq_ldm
(
  .AUXSDO             (aux_sdi_out_ldm),
  .DATAOUT(),
  .DATAOUT2(),
  .DOUT               (ioi_ldm),
  .DQSOUTN(),
  .DQSOUTP(),
  .SDO(),
  .TOUT               (t_ldm),
  .ADD                (ioi_drp_add),
  .AUXADDR            (ioi_drp_addr),
  .AUXSDOIN           (aux_sdi_out_4),
  .BKST               (ioi_drp_broadcast),
  .CLK                (ioi_drp_clk),
  .CS                 (ioi_drp_cs),
  .IDATAIN(),
  .IOCLK0             (ioclk90),
  .IOCLK1(1'b0),
  .MEMUPDATE          (ioi_drp_update),
  .ODATAIN            (ldm_oq),
  .SDI                (ioi_drp_sdo),
  .T                  (ldm_t) 
);

end
endgenerate

 ////////////////////////////////////////////////
 //IOBs instantiations
 // this part need more inputs from design team 
 // for now just use as listed in fpga.v
 ////////////////////////////////////////////////


//// Address

genvar addr_i;
   generate 
      for(addr_i = 0; addr_i < C_MEM_ADDR_WIDTH; addr_i = addr_i + 1) begin : gen_addr_obuft
        OBUFT iob_addr_inst
        (.I  ( ioi_addr[addr_i]), 
         .T   ( t_addr[addr_i]), 
         .O ( mcbx_dram_addr[addr_i])
        );
      end       
   endgenerate

genvar ba_i;
   generate 
      for(ba_i = 0; ba_i < C_MEM_BANKADDR_WIDTH; ba_i = ba_i + 1) begin : gen_ba_obuft
        OBUFT iob_ba_inst
        (.I  ( ioi_ba[ba_i]), 
         .T   ( t_ba[ba_i]), 
         .O ( mcbx_dram_ba[ba_i])
        );
      end       
   endgenerate



// DRAM Control
OBUFT iob_ras (.O(mcbx_dram_ras_n),.I(ioi_ras),.T(t_ras));
OBUFT iob_cas (.O(mcbx_dram_cas_n),.I(ioi_cas),.T(t_cas));
OBUFT iob_we  (.O(mcbx_dram_we_n ),.I(ioi_we ),.T(t_we ));
OBUFT iob_cke (.O(mcbx_dram_cke),.I(ioi_cke),.T(t_cke));

generate 
if (C_MEM_TYPE == "DDR3") begin : gen_ddr3_rst
OBUFT iob_rst (.O(mcbx_dram_ddr3_rst),.I(ioi_rst),.T(t_rst));
end
endgenerate
generate
if((C_MEM_TYPE == "DDR3"  && (C_MEM_DDR3_RTT != "OFF" || C_MEM_DDR3_DYN_WRT_ODT != "OFF"))
 ||(C_MEM_TYPE == "DDR2" &&  C_MEM_DDR2_RTT != "OFF") ) begin : gen_dram_odt
OBUFT iob_odt (.O(mcbx_dram_odt),.I(ioi_odt),.T(t_odt));
end
endgenerate

// Clock
OBUFTDS iob_clk  (.I(ioi_ck), .T(t_ck), .O(mcbx_dram_clk), .OB(mcbx_dram_clk_n)); 

//DQ
genvar dq_i;
generate
      for(dq_i = 0; dq_i < C_NUM_DQ_PINS; dq_i = dq_i + 1) begin : gen_dq_iobuft
         IOBUF gen_iob_dq_inst (.IO(mcbx_dram_dq[dq_i]),.I(ioi_dq[dq_i]),.T(t_dq[dq_i]),.O(in_pre_dq[dq_i]));
      end
endgenerate


// DQS
generate 
if(C_MEM_TYPE == "DDR" || C_MEM_TYPE =="MDDR" || (C_MEM_TYPE == "DDR2" && (C_MEM_DDR2_DIFF_DQS_EN == "NO"))) begin: gen_dqs_iobuf
IOBUF iob_dqs  (.IO(mcbx_dram_dqs), .I(ioi_dqs),.T(t_dqs),.O(in_pre_dqsp));
end else begin: gen_dqs_iobufds
IOBUFDS iob_dqs  (.IO(mcbx_dram_dqs),.IOB(mcbx_dram_dqs_n), .I(ioi_dqs),.T(t_dqs),.O(in_pre_dqsp));

end
endgenerate

generate
if((C_MEM_TYPE == "DDR" || C_MEM_TYPE =="MDDR" || (C_MEM_TYPE == "DDR2" && (C_MEM_DDR2_DIFF_DQS_EN == "NO"))) && C_NUM_DQ_PINS == 16) begin: gen_udqs_iobuf
IOBUF iob_udqs  (.IO(mcbx_dram_udqs), .I(ioi_udqs),.T(t_udqs),.O(in_pre_udqsp));
end else if(C_NUM_DQ_PINS == 16) begin: gen_udqs_iobufds
IOBUFDS iob_udqs  (.IO(mcbx_dram_udqs),.IOB(mcbx_dram_udqs_n), .I(ioi_udqs),.T(t_udqs),.O(in_pre_udqsp));

end
endgenerate

// DQS PULLDWON
generate 
if(C_MEM_TYPE == "DDR" || C_MEM_TYPE =="MDDR" || (C_MEM_TYPE == "DDR2" && (C_MEM_DDR2_DIFF_DQS_EN == "NO"))) begin: gen_dqs_pullupdn
PULLDOWN dqs_pulldown (.O(mcbx_dram_dqs));
end else begin: gen_dqs_pullupdn_ds
PULLDOWN dqs_pulldown (.O(mcbx_dram_dqs));
PULLUP dqs_n_pullup (.O(mcbx_dram_dqs_n));

end
endgenerate

// DQSN PULLUP
generate
if((C_MEM_TYPE == "DDR" || C_MEM_TYPE =="MDDR" || (C_MEM_TYPE == "DDR2" && (C_MEM_DDR2_DIFF_DQS_EN == "NO"))) && C_NUM_DQ_PINS == 16) begin: gen_udqs_pullupdn
PULLDOWN udqs_pulldown (.O(mcbx_dram_udqs));
end else if(C_NUM_DQ_PINS == 16) begin: gen_udqs_pullupdn_ds
PULLDOWN udqs_pulldown (.O(mcbx_dram_udqs));
PULLUP   udqs_n_pullup (.O(mcbx_dram_udqs_n));

end
endgenerate




//DM
//  datamask generation
generate
if( C_NUM_DQ_PINS == 16) begin : gen_udm
OBUFT iob_udm (.I(ioi_udm), .T(t_udm), .O(mcbx_dram_udm)); 
end
endgenerate

OBUFT iob_ldm (.I(ioi_ldm), .T(t_ldm), .O(mcbx_dram_ldm)); 

endmodule

module mcb_soft_calibration_top  # (
  parameter       C_MEM_TZQINIT_MAXCNT  = 10'h512,  // DDR3 Minimum delay between resets
  parameter       C_MC_CALIBRATION_MODE = "CALIBRATION", // if set to CALIBRATION will reset DQS IDELAY to DQS_NUMERATOR/DQS_DENOMINATOR local_param values, and does dynamic recal,
                                                         // if set to NOCALIBRATION then defaults to hard cal blocks setting of C_MC_CALBRATION_DELAY *and* no dynamic recal will be done 
  parameter       SKIP_IN_TERM_CAL  = 1'b0,     // provides option to skip the input termination calibration
  parameter       SKIP_DYNAMIC_CAL  = 1'b0,     // provides option to skip the dynamic delay calibration
  parameter       SKIP_DYN_IN_TERM  = 1'b0,     // provides option to skip the input termination calibration
  parameter       C_SIMULATION      = "FALSE",  // Tells us whether the design is being simulated or implemented
  parameter       C_MEM_TYPE        = "DDR"	// provides the memory device used for the design
  )
  (
  input   wire        UI_CLK,                 // Input - global clock to be used for input_term_tuner and IODRP clock
  input   wire        RST,                    // Input - reset for input_term_tuner - synchronous for input_term_tuner state machine, asynch for IODRP (sub)controller
  input   wire        IOCLK,                  // Input - IOCLK input to the IODRP's
  output  wire        DONE_SOFTANDHARD_CAL,   // active high flag signals soft calibration of input delays is complete and MCB_UODONECAL is high (MCB hard calib complete)
  input   wire        PLL_LOCK,               // Lock signal from PLL
  input   wire        SELFREFRESH_REQ,     
  input   wire        SELFREFRESH_MCB_MODE,
  output  wire         SELFREFRESH_MCB_REQ ,
  output  wire         SELFREFRESH_MODE,    
  
  
  
  
  output  wire        MCB_UIADD,              // to MCB's UIADD port
  output  wire        MCB_UISDI,              // to MCB's UISDI port
  input   wire        MCB_UOSDO,
  input   wire        MCB_UODONECAL,
  input   wire        MCB_UOREFRSHFLAG,
  output  wire        MCB_UICS,
  output  wire        MCB_UIDRPUPDATE,
  output  wire        MCB_UIBROADCAST,
  output  wire  [4:0] MCB_UIADDR,
  output  wire        MCB_UICMDEN,
  output  wire        MCB_UIDONECAL,
  output  wire        MCB_UIDQLOWERDEC,
  output  wire        MCB_UIDQLOWERINC,
  output  wire        MCB_UIDQUPPERDEC,
  output  wire        MCB_UIDQUPPERINC,
  output  wire        MCB_UILDQSDEC,
  output  wire        MCB_UILDQSINC,
  output  wire        MCB_UIREAD,
  output  wire        MCB_UIUDQSDEC,
  output  wire        MCB_UIUDQSINC,
  output  wire        MCB_RECAL,
  output  wire        MCB_SYSRST,
  output  wire        MCB_UICMD,
  output  wire        MCB_UICMDIN,
  output  wire  [3:0] MCB_UIDQCOUNT,
  input   wire  [7:0] MCB_UODATA,
  input   wire        MCB_UODATAVALID,
  input   wire        MCB_UOCMDREADY,
  input   wire        MCB_UO_CAL_START,
  
  inout   wire        RZQ_Pin,
  inout   wire        ZIO_Pin,
  output  wire            CKE_Train
  
  );

  wire IODRP_ADD;
  wire IODRP_SDI;
  wire RZQ_IODRP_SDO;
  wire RZQ_IODRP_CS;
  wire ZIO_IODRP_SDO;
  wire ZIO_IODRP_CS;
  wire IODRP_SDO;
  wire IODRP_CS;
  wire IODRP_BKST;
  wire RZQ_ZIO_ODATAIN;
  wire RZQ_ZIO_TRISTATE;
  wire RZQ_TOUT;
  wire ZIO_TOUT;
  wire [7:0] Max_Value;
  wire ZIO_IN;
  wire RZQ_IN;
  reg     ZIO_IN_R1, ZIO_IN_R2;
  reg     RZQ_IN_R1, RZQ_IN_R2;
  assign RZQ_ZIO_ODATAIN  = ~RST;
  assign RZQ_ZIO_TRISTATE = ~RST;
  assign IODRP_BKST       = 1'b0;  //future hook for possible BKST to ZIO and RZQ


mcb_soft_calibration #(
  .C_MEM_TZQINIT_MAXCNT (C_MEM_TZQINIT_MAXCNT),
  .C_MC_CALIBRATION_MODE(C_MC_CALIBRATION_MODE),
  .SKIP_IN_TERM_CAL     (SKIP_IN_TERM_CAL),
  .SKIP_DYNAMIC_CAL     (SKIP_DYNAMIC_CAL),
  .SKIP_DYN_IN_TERM     (SKIP_DYN_IN_TERM),
  .C_SIMULATION         (C_SIMULATION),
  .C_MEM_TYPE           (C_MEM_TYPE)
  ) 
mcb_soft_calibration_inst (
  .UI_CLK               (UI_CLK),  // main clock input for logic and IODRP CLK pins.  At top level, this should also connect to IODRP2_MCB CLK pins
  .RST                  (RST),             // main system reset for both this Soft Calibration block - also will act as a passthrough to MCB's SYSRST
  .PLL_LOCK             (PLL_LOCK), //lock signal from PLL
  .SELFREFRESH_REQ      (SELFREFRESH_REQ),    
  .SELFREFRESH_MCB_MODE  (SELFREFRESH_MCB_MODE),
  .SELFREFRESH_MCB_REQ   (SELFREFRESH_MCB_REQ ),
  .SELFREFRESH_MODE     (SELFREFRESH_MODE),   
  
  .DONE_SOFTANDHARD_CAL (DONE_SOFTANDHARD_CAL),// active high flag signals soft calibration of input delays is complete and MCB_UODONECAL is high (MCB hard calib complete)        .IODRP_ADD(IODRP_ADD),       // RZQ and ZIO IODRP ADD port, and MCB's UIADD port
  .IODRP_ADD            (IODRP_ADD),       // RZQ and ZIO IODRP ADD port
  .IODRP_SDI            (IODRP_SDI),       // RZQ and ZIO IODRP SDI port, and MCB's UISDI port
  .RZQ_IN               (RZQ_IN_R2),         // RZQ pin from board - expected to have a 2*R resistor to ground
  .RZQ_IODRP_SDO        (RZQ_IODRP_SDO),   // RZQ IODRP's SDO port
  .RZQ_IODRP_CS         (RZQ_IODRP_CS),   // RZQ IODRP's CS port
  .ZIO_IN               (ZIO_IN_R2),         // Z-stated IO pin - garanteed not to be driven externally
  .ZIO_IODRP_SDO        (ZIO_IODRP_SDO),   // ZIO IODRP's SDO port
  .ZIO_IODRP_CS         (ZIO_IODRP_CS),   // ZIO IODRP's CS port
  .MCB_UIADD            (MCB_UIADD),      // to MCB's UIADD port
  .MCB_UISDI            (MCB_UISDI),      // to MCB's UISDI port
  .MCB_UOSDO            (MCB_UOSDO),      // from MCB's UOSDO port (User output SDO)
  .MCB_UODONECAL        (MCB_UODONECAL), // indicates when MCB hard calibration process is complete
  .MCB_UOREFRSHFLAG     (MCB_UOREFRSHFLAG), //high during refresh cycle and time when MCB is innactive
  .MCB_UICS             (MCB_UICS),         // to MCB's UICS port (User Input CS)
  .MCB_UIDRPUPDATE      (MCB_UIDRPUPDATE),  // MCB's UIDRPUPDATE port (gets passed to IODRP2_MCB's MEMUPDATE port: this controls shadow latch used during IODRP2_MCB writes).  Currently just trasnparent
  .MCB_UIBROADCAST      (MCB_UIBROADCAST),  // to MCB's UIBROADCAST port (User Input BROADCAST - gets passed to IODRP2_MCB's BKST port)
  .MCB_UIADDR           (MCB_UIADDR),        //to MCB's UIADDR port (gets passed to IODRP2_MCB's AUXADDR port
  .MCB_UICMDEN          (MCB_UICMDEN),       //set to take control of UI interface - removes control from internal calib block
  .MCB_UIDONECAL        (MCB_UIDONECAL),
  .MCB_UIDQLOWERDEC     (MCB_UIDQLOWERDEC),
  .MCB_UIDQLOWERINC     (MCB_UIDQLOWERINC),
  .MCB_UIDQUPPERDEC     (MCB_UIDQUPPERDEC),
  .MCB_UIDQUPPERINC     (MCB_UIDQUPPERINC),
  .MCB_UILDQSDEC        (MCB_UILDQSDEC),
  .MCB_UILDQSINC        (MCB_UILDQSINC),
  .MCB_UIREAD           (MCB_UIREAD),        //enables read w/o writing by turning on a SDO->SDI loopback inside the IODRP2_MCBs (doesn't exist in regular IODRP2).  IODRPCTRLR_R_WB becomes don't-care.
  .MCB_UIUDQSDEC        (MCB_UIUDQSDEC),
  .MCB_UIUDQSINC        (MCB_UIUDQSINC),
  .MCB_RECAL            (MCB_RECAL),         //when high initiates a hard re-calibration sequence
  .MCB_UICMD            (MCB_UICMD        ),
  .MCB_UICMDIN          (MCB_UICMDIN      ),
  .MCB_UIDQCOUNT        (MCB_UIDQCOUNT    ),
  .MCB_UODATA           (MCB_UODATA       ),
  .MCB_UODATAVALID      (MCB_UODATAVALID  ),
  .MCB_UOCMDREADY       (MCB_UOCMDREADY   ),
  .MCB_UO_CAL_START     (MCB_UO_CAL_START),
  .MCB_SYSRST           (MCB_SYSRST       ), //drives the MCB's SYSRST pin - the main reset for MCB
  .Max_Value            (Max_Value        ),  // Maximum Tap Value from calibrated IOI
  .CKE_Train            (CKE_Train)
);



always@(posedge UI_CLK,posedge RST)
if (RST)        
   begin
        ZIO_IN_R1 <= 1'b0; 
        ZIO_IN_R2 <= 1'b0;

        RZQ_IN_R1 <= 1'b0; 
        RZQ_IN_R2 <= 1'b0;         
   end
else
   begin

        ZIO_IN_R1 <= ZIO_IN;
        ZIO_IN_R2 <= ZIO_IN_R1;
        RZQ_IN_R1 <= RZQ_IN;
        RZQ_IN_R2 <= RZQ_IN_R1;
   end

IOBUF IOBUF_RZQ (
    .O  (RZQ_IN),
    .IO (RZQ_Pin),
    .I  (RZQ_OUT),
    .T  (RZQ_TOUT)
    );

IODRP2 IODRP2_RZQ       (
      .DATAOUT(),
      .DATAOUT2(),
      .DOUT(RZQ_OUT),
      .SDO(RZQ_IODRP_SDO),
      .TOUT(RZQ_TOUT),
      .ADD(IODRP_ADD),
      .BKST(IODRP_BKST),
      .CLK(UI_CLK),
      .CS(RZQ_IODRP_CS),
      .IDATAIN(RZQ_IN),
      .IOCLK0(IOCLK),
      .IOCLK1(1'b1),
      .ODATAIN(RZQ_ZIO_ODATAIN),
      .SDI(IODRP_SDI),
      .T(RZQ_ZIO_TRISTATE)
      );


generate 
if ((C_MEM_TYPE == "DDR" || C_MEM_TYPE == "DDR2" || C_MEM_TYPE == "DDR3") &&
     (SKIP_IN_TERM_CAL == 1'b0)
     ) begin : gen_zio

IOBUF IOBUF_ZIO (
    .O  (ZIO_IN),
    .IO (ZIO_Pin),
    .I  (ZIO_OUT),
    .T  (ZIO_TOUT)
    );


IODRP2 IODRP2_ZIO       (
      .DATAOUT(),
      .DATAOUT2(),
      .DOUT(ZIO_OUT),
      .SDO(ZIO_IODRP_SDO),
      .TOUT(ZIO_TOUT),
      .ADD(IODRP_ADD),
      .BKST(IODRP_BKST),
      .CLK(UI_CLK),
      .CS(ZIO_IODRP_CS),
      .IDATAIN(ZIO_IN),
      .IOCLK0(IOCLK),
      .IOCLK1(1'b1),
      .ODATAIN(RZQ_ZIO_ODATAIN),
      .SDI(IODRP_SDI),
      .T(RZQ_ZIO_TRISTATE)
      );


end 
endgenerate
      
endmodule

module mcb_ui_top #
   (
///////////////////////////////////////////////////////////////////////////////
// Parameter Definitions
///////////////////////////////////////////////////////////////////////////////
   // Raw Wrapper Parameters
   parameter         C_MEMCLK_PERIOD           = 2500,
   parameter         C_PORT_ENABLE             = 6'b111111,
   parameter         C_MEM_ADDR_ORDER          = "BANK_ROW_COLUMN",
   parameter         C_USR_INTERFACE_MODE      = "NATIVE",
   parameter         C_ARB_ALGORITHM           = 0,
   parameter         C_ARB_NUM_TIME_SLOTS      = 12,
   parameter         C_ARB_TIME_SLOT_0         = 18'o012345,
   parameter         C_ARB_TIME_SLOT_1         = 18'o123450,
   parameter         C_ARB_TIME_SLOT_2         = 18'o234501,
   parameter         C_ARB_TIME_SLOT_3         = 18'o345012,
   parameter         C_ARB_TIME_SLOT_4         = 18'o450123,
   parameter         C_ARB_TIME_SLOT_5         = 18'o501234,
   parameter         C_ARB_TIME_SLOT_6         = 18'o012345,
   parameter         C_ARB_TIME_SLOT_7         = 18'o123450,
   parameter         C_ARB_TIME_SLOT_8         = 18'o234501,
   parameter         C_ARB_TIME_SLOT_9         = 18'o345012,
   parameter         C_ARB_TIME_SLOT_10        = 18'o450123,
   parameter         C_ARB_TIME_SLOT_11        = 18'o501234,
   parameter         C_PORT_CONFIG             = "B128",
   parameter         C_MEM_TRAS                = 45000,
   parameter         C_MEM_TRCD                = 12500,
   parameter         C_MEM_TREFI               = 7800,
   parameter         C_MEM_TRFC                = 127500,
   parameter         C_MEM_TRP                 = 12500,
   parameter         C_MEM_TWR                 = 15000,
   parameter         C_MEM_TRTP                = 7500,
   parameter         C_MEM_TWTR                = 7500,
   parameter         C_NUM_DQ_PINS             = 8,
   parameter         C_MEM_TYPE                = "DDR3",
   parameter         C_MEM_DENSITY             = "512M",
   parameter         C_MEM_BURST_LEN           = 8,
   parameter         C_MEM_CAS_LATENCY         = 4,
   parameter         C_MEM_ADDR_WIDTH          = 13,
   parameter         C_MEM_BANKADDR_WIDTH      = 3,
   parameter         C_MEM_NUM_COL_BITS        = 11,
   parameter         C_MEM_DDR3_CAS_LATENCY    = 7,
   parameter         C_MEM_MOBILE_PA_SR        = "FULL",
   parameter         C_MEM_DDR1_2_ODS          = "FULL",
   parameter         C_MEM_DDR3_ODS            = "DIV6",
   parameter         C_MEM_DDR2_RTT            = "50OHMS",
   parameter         C_MEM_DDR3_RTT            = "DIV2",
   parameter         C_MEM_MDDR_ODS            = "FULL",
   parameter         C_MEM_DDR2_DIFF_DQS_EN    = "YES",
   parameter         C_MEM_DDR2_3_PA_SR        = "OFF",
   parameter         C_MEM_DDR3_CAS_WR_LATENCY = 5,
   parameter         C_MEM_DDR3_AUTO_SR        = "ENABLED",
   parameter         C_MEM_DDR2_3_HIGH_TEMP_SR = "NORMAL",
   parameter         C_MEM_DDR3_DYN_WRT_ODT    = "OFF",
   parameter         C_MEM_TZQINIT_MAXCNT      = 10'd512,
   parameter         C_MC_CALIB_BYPASS         = "NO",
   parameter         C_MC_CALIBRATION_RA       = 15'h0000,
   parameter         C_MC_CALIBRATION_BA       = 3'h0,
   parameter         C_CALIB_SOFT_IP           = "TRUE",
   parameter         C_SKIP_IN_TERM_CAL        = 1'b0,
   parameter         C_SKIP_DYNAMIC_CAL        = 1'b0,
   parameter         C_SKIP_DYN_IN_TERM        = 1'b1,
   parameter         LDQSP_TAP_DELAY_VAL       = 0,
   parameter         UDQSP_TAP_DELAY_VAL       = 0,
   parameter         LDQSN_TAP_DELAY_VAL       = 0,
   parameter         UDQSN_TAP_DELAY_VAL       = 0,
   parameter         DQ0_TAP_DELAY_VAL         = 0,
   parameter         DQ1_TAP_DELAY_VAL         = 0,
   parameter         DQ2_TAP_DELAY_VAL         = 0,
   parameter         DQ3_TAP_DELAY_VAL         = 0,
   parameter         DQ4_TAP_DELAY_VAL         = 0,
   parameter         DQ5_TAP_DELAY_VAL         = 0,
   parameter         DQ6_TAP_DELAY_VAL         = 0,
   parameter         DQ7_TAP_DELAY_VAL         = 0,
   parameter         DQ8_TAP_DELAY_VAL         = 0,
   parameter         DQ9_TAP_DELAY_VAL         = 0,
   parameter         DQ10_TAP_DELAY_VAL        = 0,
   parameter         DQ11_TAP_DELAY_VAL        = 0,
   parameter         DQ12_TAP_DELAY_VAL        = 0,
   parameter         DQ13_TAP_DELAY_VAL        = 0,
   parameter         DQ14_TAP_DELAY_VAL        = 0,
   parameter         DQ15_TAP_DELAY_VAL        = 0,
   parameter         C_MC_CALIBRATION_CA       = 12'h000,
   parameter         C_MC_CALIBRATION_CLK_DIV  = 1,
   parameter         C_MC_CALIBRATION_MODE     = "CALIBRATION",
   parameter         C_MC_CALIBRATION_DELAY    = "HALF",
   parameter         C_SIMULATION              = "FALSE",
   parameter         C_P0_MASK_SIZE            = 4,
   parameter         C_P0_DATA_PORT_SIZE       = 32,
   parameter         C_P1_MASK_SIZE            = 4,
   parameter         C_P1_DATA_PORT_SIZE       = 32,
   parameter integer C_MCB_USE_EXTERNAL_BUFPLL = 1,
   // AXI Parameters
   parameter         C_S0_AXI_BASEADDR         = 32'h00000000,
   parameter         C_S0_AXI_HIGHADDR         = 32'h00000000,
   parameter integer C_S0_AXI_ENABLE           = 0,
   parameter integer C_S0_AXI_ID_WIDTH         = 4,
   parameter integer C_S0_AXI_ADDR_WIDTH       = 64,
   parameter integer C_S0_AXI_DATA_WIDTH       = 32,
   parameter integer C_S0_AXI_SUPPORTS_READ    = 1,
   parameter integer C_S0_AXI_SUPPORTS_WRITE   = 1,
   parameter integer C_S0_AXI_SUPPORTS_NARROW_BURST  = 1,
   parameter         C_S0_AXI_REG_EN0          = 20'h00000,
   parameter         C_S0_AXI_REG_EN1          = 20'h01000,
   parameter integer C_S0_AXI_STRICT_COHERENCY = 1,
   parameter integer C_S0_AXI_ENABLE_AP        = 0,
   parameter         C_S1_AXI_BASEADDR         = 32'h00000000,
   parameter         C_S1_AXI_HIGHADDR         = 32'h00000000,
   parameter integer C_S1_AXI_ENABLE           = 0,
   parameter integer C_S1_AXI_ID_WIDTH         = 4,
   parameter integer C_S1_AXI_ADDR_WIDTH       = 64,
   parameter integer C_S1_AXI_DATA_WIDTH       = 32,
   parameter integer C_S1_AXI_SUPPORTS_READ    = 1,
   parameter integer C_S1_AXI_SUPPORTS_WRITE   = 1,
   parameter integer C_S1_AXI_SUPPORTS_NARROW_BURST  = 1,
   parameter         C_S1_AXI_REG_EN0          = 20'h00000,
   parameter         C_S1_AXI_REG_EN1          = 20'h01000,
   parameter integer C_S1_AXI_STRICT_COHERENCY = 1,
   parameter integer C_S1_AXI_ENABLE_AP        = 0,
   parameter         C_S2_AXI_BASEADDR         = 32'h00000000,
   parameter         C_S2_AXI_HIGHADDR         = 32'h00000000,
   parameter integer C_S2_AXI_ENABLE           = 0,
   parameter integer C_S2_AXI_ID_WIDTH         = 4,
   parameter integer C_S2_AXI_ADDR_WIDTH       = 64,
   parameter integer C_S2_AXI_DATA_WIDTH       = 32,
   parameter integer C_S2_AXI_SUPPORTS_READ    = 1,
   parameter integer C_S2_AXI_SUPPORTS_WRITE   = 1,
   parameter integer C_S2_AXI_SUPPORTS_NARROW_BURST  = 1,
   parameter         C_S2_AXI_REG_EN0          = 20'h00000,
   parameter         C_S2_AXI_REG_EN1          = 20'h01000,
   parameter integer C_S2_AXI_STRICT_COHERENCY = 1,
   parameter integer C_S2_AXI_ENABLE_AP        = 0,
   parameter         C_S3_AXI_BASEADDR         = 32'h00000000,
   parameter         C_S3_AXI_HIGHADDR         = 32'h00000000,
   parameter integer C_S3_AXI_ENABLE           = 0,
   parameter integer C_S3_AXI_ID_WIDTH         = 4,
   parameter integer C_S3_AXI_ADDR_WIDTH       = 64,
   parameter integer C_S3_AXI_DATA_WIDTH       = 32,
   parameter integer C_S3_AXI_SUPPORTS_READ    = 1,
   parameter integer C_S3_AXI_SUPPORTS_WRITE   = 1,
   parameter integer C_S3_AXI_SUPPORTS_NARROW_BURST  = 1,
   parameter         C_S3_AXI_REG_EN0          = 20'h00000,
   parameter         C_S3_AXI_REG_EN1          = 20'h01000,
   parameter integer C_S3_AXI_STRICT_COHERENCY = 1,
   parameter integer C_S3_AXI_ENABLE_AP        = 0,
   parameter         C_S4_AXI_BASEADDR         = 32'h00000000,
   parameter         C_S4_AXI_HIGHADDR         = 32'h00000000,
   parameter integer C_S4_AXI_ENABLE           = 0,
   parameter integer C_S4_AXI_ID_WIDTH         = 4,
   parameter integer C_S4_AXI_ADDR_WIDTH       = 64,
   parameter integer C_S4_AXI_DATA_WIDTH       = 32,
   parameter integer C_S4_AXI_SUPPORTS_READ    = 1,
   parameter integer C_S4_AXI_SUPPORTS_WRITE   = 1,
   parameter integer C_S4_AXI_SUPPORTS_NARROW_BURST  = 1,
   parameter         C_S4_AXI_REG_EN0          = 20'h00000,
   parameter         C_S4_AXI_REG_EN1          = 20'h01000,
   parameter integer C_S4_AXI_STRICT_COHERENCY = 1,
   parameter integer C_S4_AXI_ENABLE_AP        = 0,
   parameter         C_S5_AXI_BASEADDR         = 32'h00000000,
   parameter         C_S5_AXI_HIGHADDR         = 32'h00000000,
   parameter integer C_S5_AXI_ENABLE           = 0,
   parameter integer C_S5_AXI_ID_WIDTH         = 4,
   parameter integer C_S5_AXI_ADDR_WIDTH       = 64,
   parameter integer C_S5_AXI_DATA_WIDTH       = 32,
   parameter integer C_S5_AXI_SUPPORTS_READ    = 1,
   parameter integer C_S5_AXI_SUPPORTS_WRITE   = 1,
   parameter integer C_S5_AXI_SUPPORTS_NARROW_BURST  = 1,
   parameter         C_S5_AXI_REG_EN0          = 20'h00000,
   parameter         C_S5_AXI_REG_EN1          = 20'h01000,
   parameter integer C_S5_AXI_STRICT_COHERENCY = 1,
   parameter integer C_S5_AXI_ENABLE_AP        = 0
   )
   (
///////////////////////////////////////////////////////////////////////////////
// Port Declarations
///////////////////////////////////////////////////////////////////////////////
   // Raw Wrapper Signals
   input                                     sysclk_2x          ,
   input                                     sysclk_2x_180      ,
   input                                     pll_ce_0           ,
   input                                     pll_ce_90          ,
   output                                    sysclk_2x_bufpll_o ,
   output                                    sysclk_2x_180_bufpll_o,
   output                                    pll_ce_0_bufpll_o  ,
   output                                    pll_ce_90_bufpll_o ,
   output                                    pll_lock_bufpll_o  ,
   input                                     pll_lock           ,
   input                                     sys_rst            ,
   input                                     p0_arb_en          ,
   input                                     p0_cmd_clk         ,
   input                                     p0_cmd_en          ,
   input       [2:0]                         p0_cmd_instr       ,
   input       [5:0]                         p0_cmd_bl          ,
   input       [29:0]                        p0_cmd_byte_addr   ,
   output                                    p0_cmd_empty       ,
   output                                    p0_cmd_full        ,
   input                                     p0_wr_clk          ,
   input                                     p0_wr_en           ,
   input       [C_P0_MASK_SIZE-1:0]          p0_wr_mask         ,
   input       [C_P0_DATA_PORT_SIZE-1:0]     p0_wr_data         ,
   output                                    p0_wr_full         ,
   output                                    p0_wr_empty        ,
   output      [6:0]                         p0_wr_count        ,
   output                                    p0_wr_underrun     ,
   output                                    p0_wr_error        ,
   input                                     p0_rd_clk          ,
   input                                     p0_rd_en           ,
   output      [C_P0_DATA_PORT_SIZE-1:0]     p0_rd_data         ,
   output                                    p0_rd_full         ,
   output                                    p0_rd_empty        ,
   output      [6:0]                         p0_rd_count        ,
   output                                    p0_rd_overflow     ,
   output                                    p0_rd_error        ,
   input                                     p1_arb_en          ,
   input                                     p1_cmd_clk         ,
   input                                     p1_cmd_en          ,
   input       [2:0]                         p1_cmd_instr       ,
   input       [5:0]                         p1_cmd_bl          ,
   input       [29:0]                        p1_cmd_byte_addr   ,
   output                                    p1_cmd_empty       ,
   output                                    p1_cmd_full        ,
   input                                     p1_wr_clk          ,
   input                                     p1_wr_en           ,
   input       [C_P1_MASK_SIZE-1:0]          p1_wr_mask         ,
   input       [C_P1_DATA_PORT_SIZE-1:0]     p1_wr_data         ,
   output                                    p1_wr_full         ,
   output                                    p1_wr_empty        ,
   output      [6:0]                         p1_wr_count        ,
   output                                    p1_wr_underrun     ,
   output                                    p1_wr_error        ,
   input                                     p1_rd_clk          ,
   input                                     p1_rd_en           ,
   output      [C_P1_DATA_PORT_SIZE-1:0]     p1_rd_data         ,
   output                                    p1_rd_full         ,
   output                                    p1_rd_empty        ,
   output      [6:0]                         p1_rd_count        ,
   output                                    p1_rd_overflow     ,
   output                                    p1_rd_error        ,
   input                                     p2_arb_en          ,
   input                                     p2_cmd_clk         ,
   input                                     p2_cmd_en          ,
   input       [2:0]                         p2_cmd_instr       ,
   input       [5:0]                         p2_cmd_bl          ,
   input       [29:0]                        p2_cmd_byte_addr   ,
   output                                    p2_cmd_empty       ,
   output                                    p2_cmd_full        ,
   input                                     p2_wr_clk          ,
   input                                     p2_wr_en           ,
   input       [3:0]                         p2_wr_mask         ,
   input       [31:0]                        p2_wr_data         ,
   output                                    p2_wr_full         ,
   output                                    p2_wr_empty        ,
   output      [6:0]                         p2_wr_count        ,
   output                                    p2_wr_underrun     ,
   output                                    p2_wr_error        ,
   input                                     p2_rd_clk          ,
   input                                     p2_rd_en           ,
   output      [31:0]                        p2_rd_data         ,
   output                                    p2_rd_full         ,
   output                                    p2_rd_empty        ,
   output      [6:0]                         p2_rd_count        ,
   output                                    p2_rd_overflow     ,
   output                                    p2_rd_error        ,
   input                                     p3_arb_en          ,
   input                                     p3_cmd_clk         ,
   input                                     p3_cmd_en          ,
   input       [2:0]                         p3_cmd_instr       ,
   input       [5:0]                         p3_cmd_bl          ,
   input       [29:0]                        p3_cmd_byte_addr   ,
   output                                    p3_cmd_empty       ,
   output                                    p3_cmd_full        ,
   input                                     p3_wr_clk          ,
   input                                     p3_wr_en           ,
   input       [3:0]                         p3_wr_mask         ,
   input       [31:0]                        p3_wr_data         ,
   output                                    p3_wr_full         ,
   output                                    p3_wr_empty        ,
   output      [6:0]                         p3_wr_count        ,
   output                                    p3_wr_underrun     ,
   output                                    p3_wr_error        ,
   input                                     p3_rd_clk          ,
   input                                     p3_rd_en           ,
   output      [31:0]                        p3_rd_data         ,
   output                                    p3_rd_full         ,
   output                                    p3_rd_empty        ,
   output      [6:0]                         p3_rd_count        ,
   output                                    p3_rd_overflow     ,
   output                                    p3_rd_error        ,
   input                                     p4_arb_en          ,
   input                                     p4_cmd_clk         ,
   input                                     p4_cmd_en          ,
   input       [2:0]                         p4_cmd_instr       ,
   input       [5:0]                         p4_cmd_bl          ,
   input       [29:0]                        p4_cmd_byte_addr   ,
   output                                    p4_cmd_empty       ,
   output                                    p4_cmd_full        ,
   input                                     p4_wr_clk          ,
   input                                     p4_wr_en           ,
   input       [3:0]                         p4_wr_mask         ,
   input       [31:0]                        p4_wr_data         ,
   output                                    p4_wr_full         ,
   output                                    p4_wr_empty        ,
   output      [6:0]                         p4_wr_count        ,
   output                                    p4_wr_underrun     ,
   output                                    p4_wr_error        ,
   input                                     p4_rd_clk          ,
   input                                     p4_rd_en           ,
   output      [31:0]                        p4_rd_data         ,
   output                                    p4_rd_full         ,
   output                                    p4_rd_empty        ,
   output      [6:0]                         p4_rd_count        ,
   output                                    p4_rd_overflow     ,
   output                                    p4_rd_error        ,
   input                                     p5_arb_en          ,
   input                                     p5_cmd_clk         ,
   input                                     p5_cmd_en          ,
   input       [2:0]                         p5_cmd_instr       ,
   input       [5:0]                         p5_cmd_bl          ,
   input       [29:0]                        p5_cmd_byte_addr   ,
   output                                    p5_cmd_empty       ,
   output                                    p5_cmd_full        ,
   input                                     p5_wr_clk          ,
   input                                     p5_wr_en           ,
   input       [3:0]                         p5_wr_mask         ,
   input       [31:0]                        p5_wr_data         ,
   output                                    p5_wr_full         ,
   output                                    p5_wr_empty        ,
   output      [6:0]                         p5_wr_count        ,
   output                                    p5_wr_underrun     ,
   output                                    p5_wr_error        ,
   input                                     p5_rd_clk          ,
   input                                     p5_rd_en           ,
   output      [31:0]                        p5_rd_data         ,
   output                                    p5_rd_full         ,
   output                                    p5_rd_empty        ,
   output      [6:0]                         p5_rd_count        ,
   output                                    p5_rd_overflow     ,
   output                                    p5_rd_error        ,
   output      [C_MEM_ADDR_WIDTH-1:0]        mcbx_dram_addr     ,
   output      [C_MEM_BANKADDR_WIDTH-1:0]    mcbx_dram_ba       ,
   output                                    mcbx_dram_ras_n    ,
   output                                    mcbx_dram_cas_n    ,
   output                                    mcbx_dram_we_n     ,
   output                                    mcbx_dram_cke      ,
   output                                    mcbx_dram_clk      ,
   output                                    mcbx_dram_clk_n    ,
   inout       [C_NUM_DQ_PINS-1:0]           mcbx_dram_dq       ,
   inout                                     mcbx_dram_dqs      ,
   inout                                     mcbx_dram_dqs_n    ,
   inout                                     mcbx_dram_udqs     ,
   inout                                     mcbx_dram_udqs_n   ,
   output                                    mcbx_dram_udm      ,
   output                                    mcbx_dram_ldm      ,
   output                                    mcbx_dram_odt      ,
   output                                    mcbx_dram_ddr3_rst ,
   input                                     calib_recal        ,
   inout                                     rzq                ,
   inout                                     zio                ,
   input                                     ui_read            ,
   input                                     ui_add             ,
   input                                     ui_cs              ,
   input                                     ui_clk             ,
   input                                     ui_sdi             ,
   input       [4:0]                         ui_addr            ,
   input                                     ui_broadcast       ,
   input                                     ui_drp_update      ,
   input                                     ui_done_cal        ,
   input                                     ui_cmd             ,
   input                                     ui_cmd_in          ,
   input                                     ui_cmd_en          ,
   input       [3:0]                         ui_dqcount         ,
   input                                     ui_dq_lower_dec    ,
   input                                     ui_dq_lower_inc    ,
   input                                     ui_dq_upper_dec    ,
   input                                     ui_dq_upper_inc    ,
   input                                     ui_udqs_inc        ,
   input                                     ui_udqs_dec        ,
   input                                     ui_ldqs_inc        ,
   input                                     ui_ldqs_dec        ,
   output      [7:0]                         uo_data            ,
   output                                    uo_data_valid      ,
   output                                    uo_done_cal        ,
   output                                    uo_cmd_ready_in    ,
   output                                    uo_refrsh_flag     ,
   output                                    uo_cal_start       ,
   output                                    uo_sdo             ,
   output      [31:0]                        status             ,
   input                                     selfrefresh_enter  ,
   output                                    selfrefresh_mode   ,
   // AXI Signals
   input  wire                               s0_axi_aclk        ,
   input  wire                               s0_axi_aresetn     ,
   input  wire [C_S0_AXI_ID_WIDTH-1:0]       s0_axi_awid        ,
   input  wire [C_S0_AXI_ADDR_WIDTH-1:0]     s0_axi_awaddr      ,
   input  wire [7:0]                         s0_axi_awlen       ,
   input  wire [2:0]                         s0_axi_awsize      ,
   input  wire [1:0]                         s0_axi_awburst     ,
   input  wire [0:0]                         s0_axi_awlock      ,
   input  wire [3:0]                         s0_axi_awcache     ,
   input  wire [2:0]                         s0_axi_awprot      ,
   input  wire [3:0]                         s0_axi_awqos       ,
   input  wire                               s0_axi_awvalid     ,
   output wire                               s0_axi_awready     ,
   input  wire [C_S0_AXI_DATA_WIDTH-1:0]     s0_axi_wdata       ,
   input  wire [C_S0_AXI_DATA_WIDTH/8-1:0]   s0_axi_wstrb       ,
   input  wire                               s0_axi_wlast       ,
   input  wire                               s0_axi_wvalid      ,
   output wire                               s0_axi_wready      ,
   output wire [C_S0_AXI_ID_WIDTH-1:0]       s0_axi_bid         ,
   output wire [1:0]                         s0_axi_bresp       ,
   output wire                               s0_axi_bvalid      ,
   input  wire                               s0_axi_bready      ,
   input  wire [C_S0_AXI_ID_WIDTH-1:0]       s0_axi_arid        ,
   input  wire [C_S0_AXI_ADDR_WIDTH-1:0]     s0_axi_araddr      ,
   input  wire [7:0]                         s0_axi_arlen       ,
   input  wire [2:0]                         s0_axi_arsize      ,
   input  wire [1:0]                         s0_axi_arburst     ,
   input  wire [0:0]                         s0_axi_arlock      ,
   input  wire [3:0]                         s0_axi_arcache     ,
   input  wire [2:0]                         s0_axi_arprot      ,
   input  wire [3:0]                         s0_axi_arqos       ,
   input  wire                               s0_axi_arvalid     ,
   output wire                               s0_axi_arready     ,
   output wire [C_S0_AXI_ID_WIDTH-1:0]       s0_axi_rid         ,
   output wire [C_S0_AXI_DATA_WIDTH-1:0]     s0_axi_rdata       ,
   output wire [1:0]                         s0_axi_rresp       ,
   output wire                               s0_axi_rlast       ,
   output wire                               s0_axi_rvalid      ,
   input  wire                               s0_axi_rready      ,

   input  wire                               s1_axi_aclk        ,
   input  wire                               s1_axi_aresetn     ,
   input  wire [C_S1_AXI_ID_WIDTH-1:0]       s1_axi_awid        ,
   input  wire [C_S1_AXI_ADDR_WIDTH-1:0]     s1_axi_awaddr      ,
   input  wire [7:0]                         s1_axi_awlen       ,
   input  wire [2:0]                         s1_axi_awsize      ,
   input  wire [1:0]                         s1_axi_awburst     ,
   input  wire [0:0]                         s1_axi_awlock      ,
   input  wire [3:0]                         s1_axi_awcache     ,
   input  wire [2:0]                         s1_axi_awprot      ,
   input  wire [3:0]                         s1_axi_awqos       ,
   input  wire                               s1_axi_awvalid     ,
   output wire                               s1_axi_awready     ,
   input  wire [C_S1_AXI_DATA_WIDTH-1:0]     s1_axi_wdata       ,
   input  wire [C_S1_AXI_DATA_WIDTH/8-1:0]   s1_axi_wstrb       ,
   input  wire                               s1_axi_wlast       ,
   input  wire                               s1_axi_wvalid      ,
   output wire                               s1_axi_wready      ,
   output wire [C_S1_AXI_ID_WIDTH-1:0]       s1_axi_bid         ,
   output wire [1:0]                         s1_axi_bresp       ,
   output wire                               s1_axi_bvalid      ,
   input  wire                               s1_axi_bready      ,
   input  wire [C_S1_AXI_ID_WIDTH-1:0]       s1_axi_arid        ,
   input  wire [C_S1_AXI_ADDR_WIDTH-1:0]     s1_axi_araddr      ,
   input  wire [7:0]                         s1_axi_arlen       ,
   input  wire [2:0]                         s1_axi_arsize      ,
   input  wire [1:0]                         s1_axi_arburst     ,
   input  wire [0:0]                         s1_axi_arlock      ,
   input  wire [3:0]                         s1_axi_arcache     ,
   input  wire [2:0]                         s1_axi_arprot      ,
   input  wire [3:0]                         s1_axi_arqos       ,
   input  wire                               s1_axi_arvalid     ,
   output wire                               s1_axi_arready     ,
   output wire [C_S1_AXI_ID_WIDTH-1:0]       s1_axi_rid         ,
   output wire [C_S1_AXI_DATA_WIDTH-1:0]     s1_axi_rdata       ,
   output wire [1:0]                         s1_axi_rresp       ,
   output wire                               s1_axi_rlast       ,
   output wire                               s1_axi_rvalid      ,
   input  wire                               s1_axi_rready      ,

   input  wire                               s2_axi_aclk        ,
   input  wire                               s2_axi_aresetn     ,
   input  wire [C_S2_AXI_ID_WIDTH-1:0]       s2_axi_awid        ,
   input  wire [C_S2_AXI_ADDR_WIDTH-1:0]     s2_axi_awaddr      ,
   input  wire [7:0]                         s2_axi_awlen       ,
   input  wire [2:0]                         s2_axi_awsize      ,
   input  wire [1:0]                         s2_axi_awburst     ,
   input  wire [0:0]                         s2_axi_awlock      ,
   input  wire [3:0]                         s2_axi_awcache     ,
   input  wire [2:0]                         s2_axi_awprot      ,
   input  wire [3:0]                         s2_axi_awqos       ,
   input  wire                               s2_axi_awvalid     ,
   output wire                               s2_axi_awready     ,
   input  wire [C_S2_AXI_DATA_WIDTH-1:0]     s2_axi_wdata       ,
   input  wire [C_S2_AXI_DATA_WIDTH/8-1:0]   s2_axi_wstrb       ,
   input  wire                               s2_axi_wlast       ,
   input  wire                               s2_axi_wvalid      ,
   output wire                               s2_axi_wready      ,
   output wire [C_S2_AXI_ID_WIDTH-1:0]       s2_axi_bid         ,
   output wire [1:0]                         s2_axi_bresp       ,
   output wire                               s2_axi_bvalid      ,
   input  wire                               s2_axi_bready      ,
   input  wire [C_S2_AXI_ID_WIDTH-1:0]       s2_axi_arid        ,
   input  wire [C_S2_AXI_ADDR_WIDTH-1:0]     s2_axi_araddr      ,
   input  wire [7:0]                         s2_axi_arlen       ,
   input  wire [2:0]                         s2_axi_arsize      ,
   input  wire [1:0]                         s2_axi_arburst     ,
   input  wire [0:0]                         s2_axi_arlock      ,
   input  wire [3:0]                         s2_axi_arcache     ,
   input  wire [2:0]                         s2_axi_arprot      ,
   input  wire [3:0]                         s2_axi_arqos       ,
   input  wire                               s2_axi_arvalid     ,
   output wire                               s2_axi_arready     ,
   output wire [C_S2_AXI_ID_WIDTH-1:0]       s2_axi_rid         ,
   output wire [C_S2_AXI_DATA_WIDTH-1:0]     s2_axi_rdata       ,
   output wire [1:0]                         s2_axi_rresp       ,
   output wire                               s2_axi_rlast       ,
   output wire                               s2_axi_rvalid      ,
   input  wire                               s2_axi_rready      ,

   input  wire                               s3_axi_aclk        ,
   input  wire                               s3_axi_aresetn     ,
   input  wire [C_S3_AXI_ID_WIDTH-1:0]       s3_axi_awid        ,
   input  wire [C_S3_AXI_ADDR_WIDTH-1:0]     s3_axi_awaddr      ,
   input  wire [7:0]                         s3_axi_awlen       ,
   input  wire [2:0]                         s3_axi_awsize      ,
   input  wire [1:0]                         s3_axi_awburst     ,
   input  wire [0:0]                         s3_axi_awlock      ,
   input  wire [3:0]                         s3_axi_awcache     ,
   input  wire [2:0]                         s3_axi_awprot      ,
   input  wire [3:0]                         s3_axi_awqos       ,
   input  wire                               s3_axi_awvalid     ,
   output wire                               s3_axi_awready     ,
   input  wire [C_S3_AXI_DATA_WIDTH-1:0]     s3_axi_wdata       ,
   input  wire [C_S3_AXI_DATA_WIDTH/8-1:0]   s3_axi_wstrb       ,
   input  wire                               s3_axi_wlast       ,
   input  wire                               s3_axi_wvalid      ,
   output wire                               s3_axi_wready      ,
   output wire [C_S3_AXI_ID_WIDTH-1:0]       s3_axi_bid         ,
   output wire [1:0]                         s3_axi_bresp       ,
   output wire                               s3_axi_bvalid      ,
   input  wire                               s3_axi_bready      ,
   input  wire [C_S3_AXI_ID_WIDTH-1:0]       s3_axi_arid        ,
   input  wire [C_S3_AXI_ADDR_WIDTH-1:0]     s3_axi_araddr      ,
   input  wire [7:0]                         s3_axi_arlen       ,
   input  wire [2:0]                         s3_axi_arsize      ,
   input  wire [1:0]                         s3_axi_arburst     ,
   input  wire [0:0]                         s3_axi_arlock      ,
   input  wire [3:0]                         s3_axi_arcache     ,
   input  wire [2:0]                         s3_axi_arprot      ,
   input  wire [3:0]                         s3_axi_arqos       ,
   input  wire                               s3_axi_arvalid     ,
   output wire                               s3_axi_arready     ,
   output wire [C_S3_AXI_ID_WIDTH-1:0]       s3_axi_rid         ,
   output wire [C_S3_AXI_DATA_WIDTH-1:0]     s3_axi_rdata       ,
   output wire [1:0]                         s3_axi_rresp       ,
   output wire                               s3_axi_rlast       ,
   output wire                               s3_axi_rvalid      ,
   input  wire                               s3_axi_rready      ,

   input  wire                               s4_axi_aclk        ,
   input  wire                               s4_axi_aresetn     ,
   input  wire [C_S4_AXI_ID_WIDTH-1:0]       s4_axi_awid        ,
   input  wire [C_S4_AXI_ADDR_WIDTH-1:0]     s4_axi_awaddr      ,
   input  wire [7:0]                         s4_axi_awlen       ,
   input  wire [2:0]                         s4_axi_awsize      ,
   input  wire [1:0]                         s4_axi_awburst     ,
   input  wire [0:0]                         s4_axi_awlock      ,
   input  wire [3:0]                         s4_axi_awcache     ,
   input  wire [2:0]                         s4_axi_awprot      ,
   input  wire [3:0]                         s4_axi_awqos       ,
   input  wire                               s4_axi_awvalid     ,
   output wire                               s4_axi_awready     ,
   input  wire [C_S4_AXI_DATA_WIDTH-1:0]     s4_axi_wdata       ,
   input  wire [C_S4_AXI_DATA_WIDTH/8-1:0]   s4_axi_wstrb       ,
   input  wire                               s4_axi_wlast       ,
   input  wire                               s4_axi_wvalid      ,
   output wire                               s4_axi_wready      ,
   output wire [C_S4_AXI_ID_WIDTH-1:0]       s4_axi_bid         ,
   output wire [1:0]                         s4_axi_bresp       ,
   output wire                               s4_axi_bvalid      ,
   input  wire                               s4_axi_bready      ,
   input  wire [C_S4_AXI_ID_WIDTH-1:0]       s4_axi_arid        ,
   input  wire [C_S4_AXI_ADDR_WIDTH-1:0]     s4_axi_araddr      ,
   input  wire [7:0]                         s4_axi_arlen       ,
   input  wire [2:0]                         s4_axi_arsize      ,
   input  wire [1:0]                         s4_axi_arburst     ,
   input  wire [0:0]                         s4_axi_arlock      ,
   input  wire [3:0]                         s4_axi_arcache     ,
   input  wire [2:0]                         s4_axi_arprot      ,
   input  wire [3:0]                         s4_axi_arqos       ,
   input  wire                               s4_axi_arvalid     ,
   output wire                               s4_axi_arready     ,
   output wire [C_S4_AXI_ID_WIDTH-1:0]       s4_axi_rid         ,
   output wire [C_S4_AXI_DATA_WIDTH-1:0]     s4_axi_rdata       ,
   output wire [1:0]                         s4_axi_rresp       ,
   output wire                               s4_axi_rlast       ,
   output wire                               s4_axi_rvalid      ,
   input  wire                               s4_axi_rready      ,

   input  wire                               s5_axi_aclk        ,
   input  wire                               s5_axi_aresetn     ,
   input  wire [C_S5_AXI_ID_WIDTH-1:0]       s5_axi_awid        ,
   input  wire [C_S5_AXI_ADDR_WIDTH-1:0]     s5_axi_awaddr      ,
   input  wire [7:0]                         s5_axi_awlen       ,
   input  wire [2:0]                         s5_axi_awsize      ,
   input  wire [1:0]                         s5_axi_awburst     ,
   input  wire [0:0]                         s5_axi_awlock      ,
   input  wire [3:0]                         s5_axi_awcache     ,
   input  wire [2:0]                         s5_axi_awprot      ,
   input  wire [3:0]                         s5_axi_awqos       ,
   input  wire                               s5_axi_awvalid     ,
   output wire                               s5_axi_awready     ,
   input  wire [C_S5_AXI_DATA_WIDTH-1:0]     s5_axi_wdata       ,
   input  wire [C_S5_AXI_DATA_WIDTH/8-1:0]   s5_axi_wstrb       ,
   input  wire                               s5_axi_wlast       ,
   input  wire                               s5_axi_wvalid      ,
   output wire                               s5_axi_wready      ,
   output wire [C_S5_AXI_ID_WIDTH-1:0]       s5_axi_bid         ,
   output wire [1:0]                         s5_axi_bresp       ,
   output wire                               s5_axi_bvalid      ,
   input  wire                               s5_axi_bready      ,
   input  wire [C_S5_AXI_ID_WIDTH-1:0]       s5_axi_arid        ,
   input  wire [C_S5_AXI_ADDR_WIDTH-1:0]     s5_axi_araddr      ,
   input  wire [7:0]                         s5_axi_arlen       ,
   input  wire [2:0]                         s5_axi_arsize      ,
   input  wire [1:0]                         s5_axi_arburst     ,
   input  wire [0:0]                         s5_axi_arlock      ,
   input  wire [3:0]                         s5_axi_arcache     ,
   input  wire [2:0]                         s5_axi_arprot      ,
   input  wire [3:0]                         s5_axi_arqos       ,
   input  wire                               s5_axi_arvalid     ,
   output wire                               s5_axi_arready     ,
   output wire [C_S5_AXI_ID_WIDTH-1:0]       s5_axi_rid         ,
   output wire [C_S5_AXI_DATA_WIDTH-1:0]     s5_axi_rdata       ,
   output wire [1:0]                         s5_axi_rresp       ,
   output wire                               s5_axi_rlast       ,
   output wire                               s5_axi_rvalid      ,
   input  wire                               s5_axi_rready
   );

////////////////////////////////////////////////////////////////////////////////
// Functions
////////////////////////////////////////////////////////////////////////////////
// Barrel Left Shift Octal
function [17:0] blso (
  input [17:0] a,
  input integer shift,
  input integer width
);
begin : func_blso
  integer i;
  integer w;
  integer s;
  w = width*3;
  s = (shift*3) % w;
  blso = 18'o000000;
  for (i = 0; i < w; i = i + 1) begin
    blso[i] = a[(i+w-s)%w];
    //bls[i] = 1'b1;
  end
end
endfunction

// For a given port_config, port_enable and slot, calculate the round robin
// arbitration that would be generated by the gui.
function [17:0] rr (
  input [5:0] port_enable,
  input integer port_config,
  input integer slot_num
);
begin : func_rr
  integer i;
  integer max_ports;
  integer num_ports;
  integer port_cnt;

  case (port_config)
    1: max_ports = 6;
    2: max_ports = 4;
    3: max_ports = 3;
    4: max_ports = 2;
    5: max_ports = 1;
// synthesis translate_off
    default : $display("ERROR: Port Config can't be %d", port_config);
// synthesis translate_on
  endcase

  num_ports = 0;
  for (i = 0; i < max_ports; i = i + 1) begin
    if (port_enable[i] == 1'b1) begin
      num_ports = num_ports + 1;
    end
  end

  rr = 18'o000000;
  port_cnt = 0;

  for (i = (num_ports-1); i >= 0; i = i - 1) begin
    while (port_enable[port_cnt] != 1'b1) begin
      port_cnt = port_cnt + 1;
    end
    rr[i*3 +: 3] = port_cnt[2:0];
    port_cnt = port_cnt +1;
  end


  rr = blso(rr, slot_num, num_ports);
end
endfunction

function [17:0] convert_arb_slot (
  input [5:0]   port_enable,
  input integer port_config,
  input [17:0]  mig_arb_slot
);
begin : func_convert_arb_slot
  integer i;
  integer num_ports;
  integer mig_port_num;
  reg [17:0] port_map;
  num_ports = 0;

  // Enumerated port configuration for ease of use
  case (port_config)
    1: port_map = 18'o543210;
    2: port_map = 18'o774210;
    3: port_map = 18'o777420;
    4: port_map = 18'o777720;
    5: port_map = 18'o777770;
// synthesis translate_off
    default : $display ("ERROR: Invalid Port Configuration.");
// synthesis translate_on
  endcase

  // Count the number of ports
  for (i = 0; i < 6; i = i + 1) begin
    if (port_enable[i] == 1'b1) begin
      num_ports = num_ports + 1;
    end
  end

  // Map the ports from the MIG GUI to the MCB Wrapper
  for (i = 0; i < 6; i = i + 1) begin
    if (i < num_ports) begin
      mig_port_num = mig_arb_slot[3*(num_ports-i-1) +: 3];
      convert_arb_slot[3*i +: 3] = port_map[3*mig_port_num +: 3];
    end else begin
      convert_arb_slot[3*i +: 3] = 3'b111;
    end
  end
end
endfunction

// Function to calculate the number of time slots automatically based on the
// number of ports used.  Will choose 10 if the number of valid ports is 5,
// otherwise it will be 12.
function integer calc_num_time_slots (
  input [5:0]   port_enable,
  input integer port_config
);
begin : func_calc_num_tim_slots
  integer num_ports;
  integer i;
  num_ports = 0;
  for (i = 0; i < 6; i = i + 1) begin
    if (port_enable[i] == 1'b1) begin
      num_ports = num_ports + 1;
    end
  end
  calc_num_time_slots = (port_config == 1 && num_ports == 5) ? 10 : 12;
end
endfunction
////////////////////////////////////////////////////////////////////////////////
// Local Parameters
////////////////////////////////////////////////////////////////////////////////
  localparam P_S0_AXI_ADDRMASK = C_S0_AXI_BASEADDR ^ C_S0_AXI_HIGHADDR;
  localparam P_S1_AXI_ADDRMASK = C_S1_AXI_BASEADDR ^ C_S1_AXI_HIGHADDR;
  localparam P_S2_AXI_ADDRMASK = C_S2_AXI_BASEADDR ^ C_S2_AXI_HIGHADDR;
  localparam P_S3_AXI_ADDRMASK = C_S3_AXI_BASEADDR ^ C_S3_AXI_HIGHADDR;
  localparam P_S4_AXI_ADDRMASK = C_S4_AXI_BASEADDR ^ C_S4_AXI_HIGHADDR;
  localparam P_S5_AXI_ADDRMASK = C_S5_AXI_BASEADDR ^ C_S5_AXI_HIGHADDR;
  localparam P_PORT_CONFIG     = (C_PORT_CONFIG == "B32_B32_B32_B32") ? 2 :
                                 (C_PORT_CONFIG == "B64_B32_B32"    ) ? 3 :
                                 (C_PORT_CONFIG == "B64_B64"        ) ? 4 :
                                 (C_PORT_CONFIG == "B128"           ) ? 5 :
                                 1; // B32_B32_x32_x32_x32_x32 case
  localparam P_ARB_NUM_TIME_SLOTS = (C_ARB_ALGORITHM == 0) ? calc_num_time_slots(C_PORT_ENABLE, P_PORT_CONFIG) : C_ARB_NUM_TIME_SLOTS;
  localparam P_0_ARB_TIME_SLOT_0 =  (C_ARB_ALGORITHM == 0) ? rr(C_PORT_ENABLE, P_PORT_CONFIG, 0 ) : C_ARB_TIME_SLOT_0 ;
  localparam P_0_ARB_TIME_SLOT_1 =  (C_ARB_ALGORITHM == 0) ? rr(C_PORT_ENABLE, P_PORT_CONFIG, 1 ) : C_ARB_TIME_SLOT_1 ;
  localparam P_0_ARB_TIME_SLOT_2 =  (C_ARB_ALGORITHM == 0) ? rr(C_PORT_ENABLE, P_PORT_CONFIG, 2 ) : C_ARB_TIME_SLOT_2 ;
  localparam P_0_ARB_TIME_SLOT_3 =  (C_ARB_ALGORITHM == 0) ? rr(C_PORT_ENABLE, P_PORT_CONFIG, 3 ) : C_ARB_TIME_SLOT_3 ;
  localparam P_0_ARB_TIME_SLOT_4 =  (C_ARB_ALGORITHM == 0) ? rr(C_PORT_ENABLE, P_PORT_CONFIG, 4 ) : C_ARB_TIME_SLOT_4 ;
  localparam P_0_ARB_TIME_SLOT_5 =  (C_ARB_ALGORITHM == 0) ? rr(C_PORT_ENABLE, P_PORT_CONFIG, 5 ) : C_ARB_TIME_SLOT_5 ;
  localparam P_0_ARB_TIME_SLOT_6 =  (C_ARB_ALGORITHM == 0) ? rr(C_PORT_ENABLE, P_PORT_CONFIG, 6 ) : C_ARB_TIME_SLOT_6 ;
  localparam P_0_ARB_TIME_SLOT_7 =  (C_ARB_ALGORITHM == 0) ? rr(C_PORT_ENABLE, P_PORT_CONFIG, 7 ) : C_ARB_TIME_SLOT_7 ;
  localparam P_0_ARB_TIME_SLOT_8 =  (C_ARB_ALGORITHM == 0) ? rr(C_PORT_ENABLE, P_PORT_CONFIG, 8 ) : C_ARB_TIME_SLOT_8 ;
  localparam P_0_ARB_TIME_SLOT_9 =  (C_ARB_ALGORITHM == 0) ? rr(C_PORT_ENABLE, P_PORT_CONFIG, 9 ) : C_ARB_TIME_SLOT_9 ;
  localparam P_0_ARB_TIME_SLOT_10 = (C_ARB_ALGORITHM == 0) ? rr(C_PORT_ENABLE, P_PORT_CONFIG, 10) : C_ARB_TIME_SLOT_10;
  localparam P_0_ARB_TIME_SLOT_11 = (C_ARB_ALGORITHM == 0) ? rr(C_PORT_ENABLE, P_PORT_CONFIG, 11) : C_ARB_TIME_SLOT_11;
  localparam P_ARB_TIME_SLOT_0 =  convert_arb_slot(C_PORT_ENABLE, P_PORT_CONFIG, P_0_ARB_TIME_SLOT_0);
  localparam P_ARB_TIME_SLOT_1 =  convert_arb_slot(C_PORT_ENABLE, P_PORT_CONFIG, P_0_ARB_TIME_SLOT_1);
  localparam P_ARB_TIME_SLOT_2 =  convert_arb_slot(C_PORT_ENABLE, P_PORT_CONFIG, P_0_ARB_TIME_SLOT_2);
  localparam P_ARB_TIME_SLOT_3 =  convert_arb_slot(C_PORT_ENABLE, P_PORT_CONFIG, P_0_ARB_TIME_SLOT_3);
  localparam P_ARB_TIME_SLOT_4 =  convert_arb_slot(C_PORT_ENABLE, P_PORT_CONFIG, P_0_ARB_TIME_SLOT_4);
  localparam P_ARB_TIME_SLOT_5 =  convert_arb_slot(C_PORT_ENABLE, P_PORT_CONFIG, P_0_ARB_TIME_SLOT_5);
  localparam P_ARB_TIME_SLOT_6 =  convert_arb_slot(C_PORT_ENABLE, P_PORT_CONFIG, P_0_ARB_TIME_SLOT_6);
  localparam P_ARB_TIME_SLOT_7 =  convert_arb_slot(C_PORT_ENABLE, P_PORT_CONFIG, P_0_ARB_TIME_SLOT_7);
  localparam P_ARB_TIME_SLOT_8 =  convert_arb_slot(C_PORT_ENABLE, P_PORT_CONFIG, P_0_ARB_TIME_SLOT_8);
  localparam P_ARB_TIME_SLOT_9 =  convert_arb_slot(C_PORT_ENABLE, P_PORT_CONFIG, P_0_ARB_TIME_SLOT_9);
  localparam P_ARB_TIME_SLOT_10 = convert_arb_slot(C_PORT_ENABLE, P_PORT_CONFIG, P_0_ARB_TIME_SLOT_10);
  localparam P_ARB_TIME_SLOT_11 = convert_arb_slot(C_PORT_ENABLE, P_PORT_CONFIG, P_0_ARB_TIME_SLOT_11);

////////////////////////////////////////////////////////////////////////////////
// Wires/Reg declarations
////////////////////////////////////////////////////////////////////////////////
  wire [C_S0_AXI_ADDR_WIDTH-1:0] s0_axi_araddr_i;
  wire [C_S0_AXI_ADDR_WIDTH-1:0] s0_axi_awaddr_i;
  wire                           p0_arb_en_i;
  wire                           p0_cmd_clk_i;
  wire                           p0_cmd_en_i;
  wire [2:0]                     p0_cmd_instr_i;
  wire [5:0]                     p0_cmd_bl_i;
  wire [29:0]                    p0_cmd_byte_addr_i;
  wire                           p0_cmd_empty_i;
  wire                           p0_cmd_full_i;
  wire                           p0_wr_clk_i;
  wire                           p0_wr_en_i;
  wire [C_P0_MASK_SIZE-1:0]      p0_wr_mask_i;
  wire [C_P0_DATA_PORT_SIZE-1:0] p0_wr_data_i;
  wire                           p0_wr_full_i;
  wire                           p0_wr_empty_i;
  wire [6:0]                     p0_wr_count_i;
  wire                           p0_wr_underrun_i;
  wire                           p0_wr_error_i;
  wire                           p0_rd_clk_i;
  wire                           p0_rd_en_i;
  wire [C_P0_DATA_PORT_SIZE-1:0] p0_rd_data_i;
  wire                           p0_rd_full_i;
  wire                           p0_rd_empty_i;
  wire [6:0]                     p0_rd_count_i;
  wire                           p0_rd_overflow_i;
  wire                           p0_rd_error_i;

  wire [C_S1_AXI_ADDR_WIDTH-1:0] s1_axi_araddr_i;
  wire [C_S1_AXI_ADDR_WIDTH-1:0] s1_axi_awaddr_i;
  wire                           p1_arb_en_i;
  wire                           p1_cmd_clk_i;
  wire                           p1_cmd_en_i;
  wire [2:0]                     p1_cmd_instr_i;
  wire [5:0]                     p1_cmd_bl_i;
  wire [29:0]                    p1_cmd_byte_addr_i;
  wire                           p1_cmd_empty_i;
  wire                           p1_cmd_full_i;
  wire                           p1_wr_clk_i;
  wire                           p1_wr_en_i;
  wire [C_P1_MASK_SIZE-1:0]      p1_wr_mask_i;
  wire [C_P1_DATA_PORT_SIZE-1:0] p1_wr_data_i;
  wire                           p1_wr_full_i;
  wire                           p1_wr_empty_i;
  wire [6:0]                     p1_wr_count_i;
  wire                           p1_wr_underrun_i;
  wire                           p1_wr_error_i;
  wire                           p1_rd_clk_i;
  wire                           p1_rd_en_i;
  wire [C_P1_DATA_PORT_SIZE-1:0] p1_rd_data_i;
  wire                           p1_rd_full_i;
  wire                           p1_rd_empty_i;
  wire [6:0]                     p1_rd_count_i;
  wire                           p1_rd_overflow_i;
  wire                           p1_rd_error_i;

  wire [C_S2_AXI_ADDR_WIDTH-1:0] s2_axi_araddr_i;
  wire [C_S2_AXI_ADDR_WIDTH-1:0] s2_axi_awaddr_i;
  wire                           p2_arb_en_i;
  wire                           p2_cmd_clk_i;
  wire                           p2_cmd_en_i;
  wire [2:0]                     p2_cmd_instr_i;
  wire [5:0]                     p2_cmd_bl_i;
  wire [29:0]                    p2_cmd_byte_addr_i;
  wire                           p2_cmd_empty_i;
  wire                           p2_cmd_full_i;
  wire                           p2_wr_clk_i;
  wire                           p2_wr_en_i;
  wire [3:0]                     p2_wr_mask_i;
  wire [31:0]                    p2_wr_data_i;
  wire                           p2_wr_full_i;
  wire                           p2_wr_empty_i;
  wire [6:0]                     p2_wr_count_i;
  wire                           p2_wr_underrun_i;
  wire                           p2_wr_error_i;
  wire                           p2_rd_clk_i;
  wire                           p2_rd_en_i;
  wire [31:0]                    p2_rd_data_i;
  wire                           p2_rd_full_i;
  wire                           p2_rd_empty_i;
  wire [6:0]                     p2_rd_count_i;
  wire                           p2_rd_overflow_i;
  wire                           p2_rd_error_i;

  wire [C_S3_AXI_ADDR_WIDTH-1:0] s3_axi_araddr_i;
  wire [C_S3_AXI_ADDR_WIDTH-1:0] s3_axi_awaddr_i;
  wire                           p3_arb_en_i;
  wire                           p3_cmd_clk_i;
  wire                           p3_cmd_en_i;
  wire [2:0]                     p3_cmd_instr_i;
  wire [5:0]                     p3_cmd_bl_i;
  wire [29:0]                    p3_cmd_byte_addr_i;
  wire                           p3_cmd_empty_i;
  wire                           p3_cmd_full_i;
  wire                           p3_wr_clk_i;
  wire                           p3_wr_en_i;
  wire [3:0]                     p3_wr_mask_i;
  wire [31:0]                    p3_wr_data_i;
  wire                           p3_wr_full_i;
  wire                           p3_wr_empty_i;
  wire [6:0]                     p3_wr_count_i;
  wire                           p3_wr_underrun_i;
  wire                           p3_wr_error_i;
  wire                           p3_rd_clk_i;
  wire                           p3_rd_en_i;
  wire [31:0]                    p3_rd_data_i;
  wire                           p3_rd_full_i;
  wire                           p3_rd_empty_i;
  wire [6:0]                     p3_rd_count_i;
  wire                           p3_rd_overflow_i;
  wire                           p3_rd_error_i;

  wire [C_S4_AXI_ADDR_WIDTH-1:0] s4_axi_araddr_i;
  wire [C_S4_AXI_ADDR_WIDTH-1:0] s4_axi_awaddr_i;
  wire                           p4_arb_en_i;
  wire                           p4_cmd_clk_i;
  wire                           p4_cmd_en_i;
  wire [2:0]                     p4_cmd_instr_i;
  wire [5:0]                     p4_cmd_bl_i;
  wire [29:0]                    p4_cmd_byte_addr_i;
  wire                           p4_cmd_empty_i;
  wire                           p4_cmd_full_i;
  wire                           p4_wr_clk_i;
  wire                           p4_wr_en_i;
  wire [3:0]                     p4_wr_mask_i;
  wire [31:0]                    p4_wr_data_i;
  wire                           p4_wr_full_i;
  wire                           p4_wr_empty_i;
  wire [6:0]                     p4_wr_count_i;
  wire                           p4_wr_underrun_i;
  wire                           p4_wr_error_i;
  wire                           p4_rd_clk_i;
  wire                           p4_rd_en_i;
  wire [31:0]                    p4_rd_data_i;
  wire                           p4_rd_full_i;
  wire                           p4_rd_empty_i;
  wire [6:0]                     p4_rd_count_i;
  wire                           p4_rd_overflow_i;
  wire                           p4_rd_error_i;

  wire [C_S5_AXI_ADDR_WIDTH-1:0] s5_axi_araddr_i;
  wire [C_S5_AXI_ADDR_WIDTH-1:0] s5_axi_awaddr_i;
  wire                           p5_arb_en_i;
  wire                           p5_cmd_clk_i;
  wire                           p5_cmd_en_i;
  wire [2:0]                     p5_cmd_instr_i;
  wire [5:0]                     p5_cmd_bl_i;
  wire [29:0]                    p5_cmd_byte_addr_i;
  wire                           p5_cmd_empty_i;
  wire                           p5_cmd_full_i;
  wire                           p5_wr_clk_i;
  wire                           p5_wr_en_i;
  wire [3:0]                     p5_wr_mask_i;
  wire [31:0]                    p5_wr_data_i;
  wire                           p5_wr_full_i;
  wire                           p5_wr_empty_i;
  wire [6:0]                     p5_wr_count_i;
  wire                           p5_wr_underrun_i;
  wire                           p5_wr_error_i;
  wire                           p5_rd_clk_i;
  wire                           p5_rd_en_i;
  wire [31:0]                    p5_rd_data_i;
  wire                           p5_rd_full_i;
  wire                           p5_rd_empty_i;
  wire [6:0]                     p5_rd_count_i;
  wire                           p5_rd_overflow_i;
  wire                           p5_rd_error_i;

  wire                           ioclk0;
  wire                           ioclk180;
  wire                           pll_ce_0_i;
  wire                           pll_ce_90_i;

  generate
    if (C_MCB_USE_EXTERNAL_BUFPLL == 0) begin : gen_spartan6_bufpll_mcb
      // Instantiate the PLL for MCB.
      BUFPLL_MCB #
      (
      .DIVIDE   (2),
      .LOCK_SRC ("LOCK_TO_0")
      )
      bufpll_0
        (
        .IOCLK0       (ioclk0),
        .IOCLK1       (ioclk180),
        .GCLK         (ui_clk),
        .LOCKED       (pll_lock),
        .LOCK         (pll_lock_bufpll_o),
        .SERDESSTROBE0(pll_ce_0_i),
        .SERDESSTROBE1(pll_ce_90_i),
        .PLLIN0       (sysclk_2x),
        .PLLIN1       (sysclk_2x_180)
        );
      end else begin : gen_spartan6_no_bufpll_mcb
        // Use external bufpll_mcb.
        assign pll_ce_0_i   = pll_ce_0;
        assign pll_ce_90_i  = pll_ce_90;
        assign ioclk0     = sysclk_2x;
        assign ioclk180   = sysclk_2x_180;
        assign pll_lock_bufpll_o = pll_lock;
      end
  endgenerate

  assign sysclk_2x_bufpll_o     = ioclk0;
  assign sysclk_2x_180_bufpll_o = ioclk180;
  assign pll_ce_0_bufpll_o      = pll_ce_0_i;
  assign pll_ce_90_bufpll_o     = pll_ce_90_i;

mcb_raw_wrapper #
   (
   .C_MEMCLK_PERIOD           ( C_MEMCLK_PERIOD           ),
   .C_PORT_ENABLE             ( C_PORT_ENABLE             ),
   .C_MEM_ADDR_ORDER          ( C_MEM_ADDR_ORDER          ),
   .C_USR_INTERFACE_MODE      ( C_USR_INTERFACE_MODE      ),
   .C_ARB_NUM_TIME_SLOTS      ( P_ARB_NUM_TIME_SLOTS      ),
   .C_ARB_TIME_SLOT_0         ( P_ARB_TIME_SLOT_0         ),
   .C_ARB_TIME_SLOT_1         ( P_ARB_TIME_SLOT_1         ),
   .C_ARB_TIME_SLOT_2         ( P_ARB_TIME_SLOT_2         ),
   .C_ARB_TIME_SLOT_3         ( P_ARB_TIME_SLOT_3         ),
   .C_ARB_TIME_SLOT_4         ( P_ARB_TIME_SLOT_4         ),
   .C_ARB_TIME_SLOT_5         ( P_ARB_TIME_SLOT_5         ),
   .C_ARB_TIME_SLOT_6         ( P_ARB_TIME_SLOT_6         ),
   .C_ARB_TIME_SLOT_7         ( P_ARB_TIME_SLOT_7         ),
   .C_ARB_TIME_SLOT_8         ( P_ARB_TIME_SLOT_8         ),
   .C_ARB_TIME_SLOT_9         ( P_ARB_TIME_SLOT_9         ),
   .C_ARB_TIME_SLOT_10        ( P_ARB_TIME_SLOT_10        ),
   .C_ARB_TIME_SLOT_11        ( P_ARB_TIME_SLOT_11        ),
   .C_PORT_CONFIG             ( C_PORT_CONFIG             ),
   .C_MEM_TRAS                ( C_MEM_TRAS                ),
   .C_MEM_TRCD                ( C_MEM_TRCD                ),
   .C_MEM_TREFI               ( C_MEM_TREFI               ),
   .C_MEM_TRFC                ( C_MEM_TRFC                ),
   .C_MEM_TRP                 ( C_MEM_TRP                 ),
   .C_MEM_TWR                 ( C_MEM_TWR                 ),
   .C_MEM_TRTP                ( C_MEM_TRTP                ),
   .C_MEM_TWTR                ( C_MEM_TWTR                ),
   .C_NUM_DQ_PINS             ( C_NUM_DQ_PINS             ),
   .C_MEM_TYPE                ( C_MEM_TYPE                ),
   .C_MEM_DENSITY             ( C_MEM_DENSITY             ),
   .C_MEM_BURST_LEN           ( C_MEM_BURST_LEN           ),
   .C_MEM_CAS_LATENCY         ( C_MEM_CAS_LATENCY         ),
   .C_MEM_ADDR_WIDTH          ( C_MEM_ADDR_WIDTH          ),
   .C_MEM_BANKADDR_WIDTH      ( C_MEM_BANKADDR_WIDTH      ),
   .C_MEM_NUM_COL_BITS        ( C_MEM_NUM_COL_BITS        ),
   .C_MEM_DDR3_CAS_LATENCY    ( C_MEM_DDR3_CAS_LATENCY    ),
   .C_MEM_MOBILE_PA_SR        ( C_MEM_MOBILE_PA_SR        ),
   .C_MEM_DDR1_2_ODS          ( C_MEM_DDR1_2_ODS          ),
   .C_MEM_DDR3_ODS            ( C_MEM_DDR3_ODS            ),
   .C_MEM_DDR2_RTT            ( C_MEM_DDR2_RTT            ),
   .C_MEM_DDR3_RTT            ( C_MEM_DDR3_RTT            ),
   .C_MEM_MDDR_ODS            ( C_MEM_MDDR_ODS            ),
   .C_MEM_DDR2_DIFF_DQS_EN    ( C_MEM_DDR2_DIFF_DQS_EN    ),
   .C_MEM_DDR2_3_PA_SR        ( C_MEM_DDR2_3_PA_SR        ),
   .C_MEM_DDR3_CAS_WR_LATENCY ( C_MEM_DDR3_CAS_WR_LATENCY ),
   .C_MEM_DDR3_AUTO_SR        ( C_MEM_DDR3_AUTO_SR        ),
   .C_MEM_DDR2_3_HIGH_TEMP_SR ( C_MEM_DDR2_3_HIGH_TEMP_SR ),
   .C_MEM_DDR3_DYN_WRT_ODT    ( C_MEM_DDR3_DYN_WRT_ODT    ),
   // Subtract 16 to stop TRFC violations.
   .C_MEM_TZQINIT_MAXCNT      ( C_MEM_TZQINIT_MAXCNT - 16 ),
   .C_MC_CALIB_BYPASS         ( C_MC_CALIB_BYPASS         ),
   .C_MC_CALIBRATION_RA       ( C_MC_CALIBRATION_RA       ),
   .C_MC_CALIBRATION_BA       ( C_MC_CALIBRATION_BA       ),
   .C_CALIB_SOFT_IP           ( C_CALIB_SOFT_IP           ),
   .C_SKIP_IN_TERM_CAL        ( C_SKIP_IN_TERM_CAL        ),
   .C_SKIP_DYNAMIC_CAL        ( C_SKIP_DYNAMIC_CAL        ),
   .C_SKIP_DYN_IN_TERM        ( C_SKIP_DYN_IN_TERM        ),
   .LDQSP_TAP_DELAY_VAL       ( LDQSP_TAP_DELAY_VAL       ),
   .UDQSP_TAP_DELAY_VAL       ( UDQSP_TAP_DELAY_VAL       ),
   .LDQSN_TAP_DELAY_VAL       ( LDQSN_TAP_DELAY_VAL       ),
   .UDQSN_TAP_DELAY_VAL       ( UDQSN_TAP_DELAY_VAL       ),
   .DQ0_TAP_DELAY_VAL         ( DQ0_TAP_DELAY_VAL         ),
   .DQ1_TAP_DELAY_VAL         ( DQ1_TAP_DELAY_VAL         ),
   .DQ2_TAP_DELAY_VAL         ( DQ2_TAP_DELAY_VAL         ),
   .DQ3_TAP_DELAY_VAL         ( DQ3_TAP_DELAY_VAL         ),
   .DQ4_TAP_DELAY_VAL         ( DQ4_TAP_DELAY_VAL         ),
   .DQ5_TAP_DELAY_VAL         ( DQ5_TAP_DELAY_VAL         ),
   .DQ6_TAP_DELAY_VAL         ( DQ6_TAP_DELAY_VAL         ),
   .DQ7_TAP_DELAY_VAL         ( DQ7_TAP_DELAY_VAL         ),
   .DQ8_TAP_DELAY_VAL         ( DQ8_TAP_DELAY_VAL         ),
   .DQ9_TAP_DELAY_VAL         ( DQ9_TAP_DELAY_VAL         ),
   .DQ10_TAP_DELAY_VAL        ( DQ10_TAP_DELAY_VAL        ),
   .DQ11_TAP_DELAY_VAL        ( DQ11_TAP_DELAY_VAL        ),
   .DQ12_TAP_DELAY_VAL        ( DQ12_TAP_DELAY_VAL        ),
   .DQ13_TAP_DELAY_VAL        ( DQ13_TAP_DELAY_VAL        ),
   .DQ14_TAP_DELAY_VAL        ( DQ14_TAP_DELAY_VAL        ),
   .DQ15_TAP_DELAY_VAL        ( DQ15_TAP_DELAY_VAL        ),
   .C_MC_CALIBRATION_CA       ( C_MC_CALIBRATION_CA       ),
   .C_MC_CALIBRATION_CLK_DIV  ( C_MC_CALIBRATION_CLK_DIV  ),
   .C_MC_CALIBRATION_MODE     ( C_MC_CALIBRATION_MODE     ),
   .C_MC_CALIBRATION_DELAY    ( C_MC_CALIBRATION_DELAY    ),
   // synthesis translate_off
   .C_SIMULATION              ( C_SIMULATION              ),
   // synthesis translate_on
   .C_P0_MASK_SIZE            ( C_P0_MASK_SIZE            ),
   .C_P0_DATA_PORT_SIZE       ( C_P0_DATA_PORT_SIZE       ),
   .C_P1_MASK_SIZE            ( C_P1_MASK_SIZE            ),
   .C_P1_DATA_PORT_SIZE       ( C_P1_DATA_PORT_SIZE       )
   )
   mcb_raw_wrapper_inst
   (
   .sysclk_2x                 ( ioclk0                    ),
   .sysclk_2x_180             ( ioclk180                  ),
   .pll_ce_0                  ( pll_ce_0_i                ),
   .pll_ce_90                 ( pll_ce_90_i               ),
   .pll_lock                  ( pll_lock_bufpll_o         ),
   .sys_rst                   ( sys_rst                   ),
   .p0_arb_en                 ( p0_arb_en_i               ),
   .p0_cmd_clk                ( p0_cmd_clk_i              ),
   .p0_cmd_en                 ( p0_cmd_en_i               ),
   .p0_cmd_instr              ( p0_cmd_instr_i            ),
   .p0_cmd_bl                 ( p0_cmd_bl_i               ),
   .p0_cmd_byte_addr          ( p0_cmd_byte_addr_i        ),
   .p0_cmd_empty              ( p0_cmd_empty_i            ),
   .p0_cmd_full               ( p0_cmd_full_i             ),
   .p0_wr_clk                 ( p0_wr_clk_i               ),
   .p0_wr_en                  ( p0_wr_en_i                ),
   .p0_wr_mask                ( p0_wr_mask_i              ),
   .p0_wr_data                ( p0_wr_data_i              ),
   .p0_wr_full                ( p0_wr_full_i              ),
   .p0_wr_empty               ( p0_wr_empty_i             ),
   .p0_wr_count               ( p0_wr_count_i             ),
   .p0_wr_underrun            ( p0_wr_underrun_i          ),
   .p0_wr_error               ( p0_wr_error_i             ),
   .p0_rd_clk                 ( p0_rd_clk_i               ),
   .p0_rd_en                  ( p0_rd_en_i                ),
   .p0_rd_data                ( p0_rd_data_i              ),
   .p0_rd_full                ( p0_rd_full_i              ),
   .p0_rd_empty               ( p0_rd_empty_i             ),
   .p0_rd_count               ( p0_rd_count_i             ),
   .p0_rd_overflow            ( p0_rd_overflow_i          ),
   .p0_rd_error               ( p0_rd_error_i             ),
   .p1_arb_en                 ( p1_arb_en_i               ),
   .p1_cmd_clk                ( p1_cmd_clk_i              ),
   .p1_cmd_en                 ( p1_cmd_en_i               ),
   .p1_cmd_instr              ( p1_cmd_instr_i            ),
   .p1_cmd_bl                 ( p1_cmd_bl_i               ),
   .p1_cmd_byte_addr          ( p1_cmd_byte_addr_i        ),
   .p1_cmd_empty              ( p1_cmd_empty_i            ),
   .p1_cmd_full               ( p1_cmd_full_i             ),
   .p1_wr_clk                 ( p1_wr_clk_i               ),
   .p1_wr_en                  ( p1_wr_en_i                ),
   .p1_wr_mask                ( p1_wr_mask_i              ),
   .p1_wr_data                ( p1_wr_data_i              ),
   .p1_wr_full                ( p1_wr_full_i              ),
   .p1_wr_empty               ( p1_wr_empty_i             ),
   .p1_wr_count               ( p1_wr_count_i             ),
   .p1_wr_underrun            ( p1_wr_underrun_i          ),
   .p1_wr_error               ( p1_wr_error_i             ),
   .p1_rd_clk                 ( p1_rd_clk_i               ),
   .p1_rd_en                  ( p1_rd_en_i                ),
   .p1_rd_data                ( p1_rd_data_i              ),
   .p1_rd_full                ( p1_rd_full_i              ),
   .p1_rd_empty               ( p1_rd_empty_i             ),
   .p1_rd_count               ( p1_rd_count_i             ),
   .p1_rd_overflow            ( p1_rd_overflow_i          ),
   .p1_rd_error               ( p1_rd_error_i             ),
   .p2_arb_en                 ( p2_arb_en_i               ),
   .p2_cmd_clk                ( p2_cmd_clk_i              ),
   .p2_cmd_en                 ( p2_cmd_en_i               ),
   .p2_cmd_instr              ( p2_cmd_instr_i            ),
   .p2_cmd_bl                 ( p2_cmd_bl_i               ),
   .p2_cmd_byte_addr          ( p2_cmd_byte_addr_i        ),
   .p2_cmd_empty              ( p2_cmd_empty_i            ),
   .p2_cmd_full               ( p2_cmd_full_i             ),
   .p2_wr_clk                 ( p2_wr_clk_i               ),
   .p2_wr_en                  ( p2_wr_en_i                ),
   .p2_wr_mask                ( p2_wr_mask_i              ),
   .p2_wr_data                ( p2_wr_data_i              ),
   .p2_wr_full                ( p2_wr_full_i              ),
   .p2_wr_empty               ( p2_wr_empty_i             ),
   .p2_wr_count               ( p2_wr_count_i             ),
   .p2_wr_underrun            ( p2_wr_underrun_i          ),
   .p2_wr_error               ( p2_wr_error_i             ),
   .p2_rd_clk                 ( p2_rd_clk_i               ),
   .p2_rd_en                  ( p2_rd_en_i                ),
   .p2_rd_data                ( p2_rd_data_i              ),
   .p2_rd_full                ( p2_rd_full_i              ),
   .p2_rd_empty               ( p2_rd_empty_i             ),
   .p2_rd_count               ( p2_rd_count_i             ),
   .p2_rd_overflow            ( p2_rd_overflow_i          ),
   .p2_rd_error               ( p2_rd_error_i             ),
   .p3_arb_en                 ( p3_arb_en_i               ),
   .p3_cmd_clk                ( p3_cmd_clk_i              ),
   .p3_cmd_en                 ( p3_cmd_en_i               ),
   .p3_cmd_instr              ( p3_cmd_instr_i            ),
   .p3_cmd_bl                 ( p3_cmd_bl_i               ),
   .p3_cmd_byte_addr          ( p3_cmd_byte_addr_i        ),
   .p3_cmd_empty              ( p3_cmd_empty_i            ),
   .p3_cmd_full               ( p3_cmd_full_i             ),
   .p3_wr_clk                 ( p3_wr_clk_i               ),
   .p3_wr_en                  ( p3_wr_en_i                ),
   .p3_wr_mask                ( p3_wr_mask_i              ),
   .p3_wr_data                ( p3_wr_data_i              ),
   .p3_wr_full                ( p3_wr_full_i              ),
   .p3_wr_empty               ( p3_wr_empty_i             ),
   .p3_wr_count               ( p3_wr_count_i             ),
   .p3_wr_underrun            ( p3_wr_underrun_i          ),
   .p3_wr_error               ( p3_wr_error_i             ),
   .p3_rd_clk                 ( p3_rd_clk_i               ),
   .p3_rd_en                  ( p3_rd_en_i                ),
   .p3_rd_data                ( p3_rd_data_i              ),
   .p3_rd_full                ( p3_rd_full_i              ),
   .p3_rd_empty               ( p3_rd_empty_i             ),
   .p3_rd_count               ( p3_rd_count_i             ),
   .p3_rd_overflow            ( p3_rd_overflow_i          ),
   .p3_rd_error               ( p3_rd_error_i             ),
   .p4_arb_en                 ( p4_arb_en_i               ),
   .p4_cmd_clk                ( p4_cmd_clk_i              ),
   .p4_cmd_en                 ( p4_cmd_en_i               ),
   .p4_cmd_instr              ( p4_cmd_instr_i            ),
   .p4_cmd_bl                 ( p4_cmd_bl_i               ),
   .p4_cmd_byte_addr          ( p4_cmd_byte_addr_i        ),
   .p4_cmd_empty              ( p4_cmd_empty_i            ),
   .p4_cmd_full               ( p4_cmd_full_i             ),
   .p4_wr_clk                 ( p4_wr_clk_i               ),
   .p4_wr_en                  ( p4_wr_en_i                ),
   .p4_wr_mask                ( p4_wr_mask_i              ),
   .p4_wr_data                ( p4_wr_data_i              ),
   .p4_wr_full                ( p4_wr_full_i              ),
   .p4_wr_empty               ( p4_wr_empty_i             ),
   .p4_wr_count               ( p4_wr_count_i             ),
   .p4_wr_underrun            ( p4_wr_underrun_i          ),
   .p4_wr_error               ( p4_wr_error_i             ),
   .p4_rd_clk                 ( p4_rd_clk_i               ),
   .p4_rd_en                  ( p4_rd_en_i                ),
   .p4_rd_data                ( p4_rd_data_i              ),
   .p4_rd_full                ( p4_rd_full_i              ),
   .p4_rd_empty               ( p4_rd_empty_i             ),
   .p4_rd_count               ( p4_rd_count_i             ),
   .p4_rd_overflow            ( p4_rd_overflow_i          ),
   .p4_rd_error               ( p4_rd_error_i             ),
   .p5_arb_en                 ( p5_arb_en_i               ),
   .p5_cmd_clk                ( p5_cmd_clk_i              ),
   .p5_cmd_en                 ( p5_cmd_en_i               ),
   .p5_cmd_instr              ( p5_cmd_instr_i            ),
   .p5_cmd_bl                 ( p5_cmd_bl_i               ),
   .p5_cmd_byte_addr          ( p5_cmd_byte_addr_i        ),
   .p5_cmd_empty              ( p5_cmd_empty_i            ),
   .p5_cmd_full               ( p5_cmd_full_i             ),
   .p5_wr_clk                 ( p5_wr_clk_i               ),
   .p5_wr_en                  ( p5_wr_en_i                ),
   .p5_wr_mask                ( p5_wr_mask_i              ),
   .p5_wr_data                ( p5_wr_data_i              ),
   .p5_wr_full                ( p5_wr_full_i              ),
   .p5_wr_empty               ( p5_wr_empty_i             ),
   .p5_wr_count               ( p5_wr_count_i             ),
   .p5_wr_underrun            ( p5_wr_underrun_i          ),
   .p5_wr_error               ( p5_wr_error_i             ),
   .p5_rd_clk                 ( p5_rd_clk_i               ),
   .p5_rd_en                  ( p5_rd_en_i                ),
   .p5_rd_data                ( p5_rd_data_i              ),
   .p5_rd_full                ( p5_rd_full_i              ),
   .p5_rd_empty               ( p5_rd_empty_i             ),
   .p5_rd_count               ( p5_rd_count_i             ),
   .p5_rd_overflow            ( p5_rd_overflow_i          ),
   .p5_rd_error               ( p5_rd_error_i             ),
   .mcbx_dram_addr            ( mcbx_dram_addr            ),
   .mcbx_dram_ba              ( mcbx_dram_ba              ),
   .mcbx_dram_ras_n           ( mcbx_dram_ras_n           ),
   .mcbx_dram_cas_n           ( mcbx_dram_cas_n           ),
   .mcbx_dram_we_n            ( mcbx_dram_we_n            ),
   .mcbx_dram_cke             ( mcbx_dram_cke             ),
   .mcbx_dram_clk             ( mcbx_dram_clk             ),
   .mcbx_dram_clk_n           ( mcbx_dram_clk_n           ),
   .mcbx_dram_dq              ( mcbx_dram_dq              ),
   .mcbx_dram_dqs             ( mcbx_dram_dqs             ),
   .mcbx_dram_dqs_n           ( mcbx_dram_dqs_n           ),
   .mcbx_dram_udqs            ( mcbx_dram_udqs            ),
   .mcbx_dram_udqs_n          ( mcbx_dram_udqs_n          ),
   .mcbx_dram_udm             ( mcbx_dram_udm             ),
   .mcbx_dram_ldm             ( mcbx_dram_ldm             ),
   .mcbx_dram_odt             ( mcbx_dram_odt             ),
   .mcbx_dram_ddr3_rst        ( mcbx_dram_ddr3_rst        ),
   .calib_recal               ( calib_recal               ),
   .rzq                       ( rzq                       ),
   .zio                       ( zio                       ),
   .ui_read                   ( ui_read                   ),
   .ui_add                    ( ui_add                    ),
   .ui_cs                     ( ui_cs                     ),
   .ui_clk                    ( ui_clk                    ),
   .ui_sdi                    ( ui_sdi                    ),
   .ui_addr                   ( ui_addr                   ),
   .ui_broadcast              ( ui_broadcast              ),
   .ui_drp_update             ( ui_drp_update             ),
   .ui_done_cal               ( ui_done_cal               ),
   .ui_cmd                    ( ui_cmd                    ),
   .ui_cmd_in                 ( ui_cmd_in                 ),
   .ui_cmd_en                 ( ui_cmd_en                 ),
   .ui_dqcount                ( ui_dqcount                ),
   .ui_dq_lower_dec           ( ui_dq_lower_dec           ),
   .ui_dq_lower_inc           ( ui_dq_lower_inc           ),
   .ui_dq_upper_dec           ( ui_dq_upper_dec           ),
   .ui_dq_upper_inc           ( ui_dq_upper_inc           ),
   .ui_udqs_inc               ( ui_udqs_inc               ),
   .ui_udqs_dec               ( ui_udqs_dec               ),
   .ui_ldqs_inc               ( ui_ldqs_inc               ),
   .ui_ldqs_dec               ( ui_ldqs_dec               ),
   .uo_data                   ( uo_data                   ),
   .uo_data_valid             ( uo_data_valid             ),
   .uo_done_cal               ( uo_done_cal               ),
   .uo_cmd_ready_in           ( uo_cmd_ready_in           ),
   .uo_refrsh_flag            ( uo_refrsh_flag            ),
   .uo_cal_start              ( uo_cal_start              ),
   .uo_sdo                    ( uo_sdo                    ),
   .status                    ( status                    ),
   .selfrefresh_enter         ( selfrefresh_enter         ),
   .selfrefresh_mode          ( selfrefresh_mode          )
   );

// P0 AXI Bridge Mux
  generate
    if (C_S0_AXI_ENABLE == 0) begin : P0_UI_MCB
      assign  p0_arb_en_i        =  p0_arb_en        ; //
      assign  p0_cmd_clk_i       =  p0_cmd_clk       ; //
      assign  p0_cmd_en_i        =  p0_cmd_en        ; //
      assign  p0_cmd_instr_i     =  p0_cmd_instr     ; // [2:0]
      assign  p0_cmd_bl_i        =  p0_cmd_bl        ; // [5:0]
      assign  p0_cmd_byte_addr_i =  p0_cmd_byte_addr ; // [29:0]
      assign  p0_cmd_empty       =  p0_cmd_empty_i   ; //
      assign  p0_cmd_full        =  p0_cmd_full_i    ; //
      assign  p0_wr_clk_i        =  p0_wr_clk        ; //
      assign  p0_wr_en_i         =  p0_wr_en         ; //
      assign  p0_wr_mask_i       =  p0_wr_mask       ; // [C_P0_MASK_SIZE-1:0]
      assign  p0_wr_data_i       =  p0_wr_data       ; // [C_P0_DATA_PORT_SIZE-1:0]
      assign  p0_wr_full         =  p0_wr_full_i     ; //
      assign  p0_wr_empty        =  p0_wr_empty_i    ; //
      assign  p0_wr_count        =  p0_wr_count_i    ; // [6:0]
      assign  p0_wr_underrun     =  p0_wr_underrun_i ; //
      assign  p0_wr_error        =  p0_wr_error_i    ; //
      assign  p0_rd_clk_i        =  p0_rd_clk        ; //
      assign  p0_rd_en_i         =  p0_rd_en         ; //
      assign  p0_rd_data         =  p0_rd_data_i     ; // [C_P0_DATA_PORT_SIZE-1:0]
      assign  p0_rd_full         =  p0_rd_full_i     ; //
      assign  p0_rd_empty        =  p0_rd_empty_i    ; //
      assign  p0_rd_count        =  p0_rd_count_i    ; // [6:0]
      assign  p0_rd_overflow     =  p0_rd_overflow_i ; //
      assign  p0_rd_error        =  p0_rd_error_i    ; //
    end
    else begin : P0_UI_AXI
      assign  p0_arb_en_i        =  p0_arb_en;
      assign  s0_axi_araddr_i    = s0_axi_araddr & P_S0_AXI_ADDRMASK;
      assign  s0_axi_awaddr_i    = s0_axi_awaddr & P_S0_AXI_ADDRMASK;
      wire                     calib_done_synch;

      mcb_ui_top_synch #(
        .C_SYNCH_WIDTH          ( 1 )
      )
      axi_mcb_synch
      (
        .clk       ( s0_axi_aclk      ) ,
        .synch_in  ( uo_done_cal      ) ,
        .synch_out ( calib_done_synch )
      );
      axi_mcb #
        (
        .C_FAMILY                ( "spartan6"               ) ,
        .C_S_AXI_ID_WIDTH        ( C_S0_AXI_ID_WIDTH        ) ,
        .C_S_AXI_ADDR_WIDTH      ( C_S0_AXI_ADDR_WIDTH      ) ,
        .C_S_AXI_DATA_WIDTH      ( C_S0_AXI_DATA_WIDTH      ) ,
        .C_S_AXI_SUPPORTS_READ   ( C_S0_AXI_SUPPORTS_READ   ) ,
        .C_S_AXI_SUPPORTS_WRITE  ( C_S0_AXI_SUPPORTS_WRITE  ) ,
        .C_S_AXI_REG_EN0         ( C_S0_AXI_REG_EN0         ) ,
        .C_S_AXI_REG_EN1         ( C_S0_AXI_REG_EN1         ) ,
        .C_S_AXI_SUPPORTS_NARROW_BURST ( C_S0_AXI_SUPPORTS_NARROW_BURST ) ,
        .C_MCB_ADDR_WIDTH        ( 30                       ) ,
        .C_MCB_DATA_WIDTH        ( C_P0_DATA_PORT_SIZE      ) ,
        .C_STRICT_COHERENCY      ( C_S0_AXI_STRICT_COHERENCY    ) ,
        .C_ENABLE_AP             ( C_S0_AXI_ENABLE_AP           )
        )
        p0_axi_mcb
        (
        .aclk              ( s0_axi_aclk        ),
        .aresetn           ( s0_axi_aresetn     ),
        .s_axi_awid        ( s0_axi_awid        ),
        .s_axi_awaddr      ( s0_axi_awaddr_i    ),
        .s_axi_awlen       ( s0_axi_awlen       ),
        .s_axi_awsize      ( s0_axi_awsize      ),
        .s_axi_awburst     ( s0_axi_awburst     ),
        .s_axi_awlock      ( s0_axi_awlock      ),
        .s_axi_awcache     ( s0_axi_awcache     ),
        .s_axi_awprot      ( s0_axi_awprot      ),
        .s_axi_awqos       ( s0_axi_awqos       ),
        .s_axi_awvalid     ( s0_axi_awvalid     ),
        .s_axi_awready     ( s0_axi_awready     ),
        .s_axi_wdata       ( s0_axi_wdata       ),
        .s_axi_wstrb       ( s0_axi_wstrb       ),
        .s_axi_wlast       ( s0_axi_wlast       ),
        .s_axi_wvalid      ( s0_axi_wvalid      ),
        .s_axi_wready      ( s0_axi_wready      ),
        .s_axi_bid         ( s0_axi_bid         ),
        .s_axi_bresp       ( s0_axi_bresp       ),
        .s_axi_bvalid      ( s0_axi_bvalid      ),
        .s_axi_bready      ( s0_axi_bready      ),
        .s_axi_arid        ( s0_axi_arid        ),
        .s_axi_araddr      ( s0_axi_araddr_i    ),
        .s_axi_arlen       ( s0_axi_arlen       ),
        .s_axi_arsize      ( s0_axi_arsize      ),
        .s_axi_arburst     ( s0_axi_arburst     ),
        .s_axi_arlock      ( s0_axi_arlock      ),
        .s_axi_arcache     ( s0_axi_arcache     ),
        .s_axi_arprot      ( s0_axi_arprot      ),
        .s_axi_arqos       ( s0_axi_arqos       ),
        .s_axi_arvalid     ( s0_axi_arvalid     ),
        .s_axi_arready     ( s0_axi_arready     ),
        .s_axi_rid         ( s0_axi_rid         ),
        .s_axi_rdata       ( s0_axi_rdata       ),
        .s_axi_rresp       ( s0_axi_rresp       ),
        .s_axi_rlast       ( s0_axi_rlast       ),
        .s_axi_rvalid      ( s0_axi_rvalid      ),
        .s_axi_rready      ( s0_axi_rready      ),
        .mcb_cmd_clk       ( p0_cmd_clk_i       ),
        .mcb_cmd_en        ( p0_cmd_en_i        ),
        .mcb_cmd_instr     ( p0_cmd_instr_i     ),
        .mcb_cmd_bl        ( p0_cmd_bl_i        ),
        .mcb_cmd_byte_addr ( p0_cmd_byte_addr_i ),
        .mcb_cmd_empty     ( p0_cmd_empty_i     ),
        .mcb_cmd_full      ( p0_cmd_full_i      ),
        .mcb_wr_clk        ( p0_wr_clk_i        ),
        .mcb_wr_en         ( p0_wr_en_i         ),
        .mcb_wr_mask       ( p0_wr_mask_i       ),
        .mcb_wr_data       ( p0_wr_data_i       ),
        .mcb_wr_full       ( p0_wr_full_i       ),
        .mcb_wr_empty      ( p0_wr_empty_i      ),
        .mcb_wr_count      ( p0_wr_count_i      ),
        .mcb_wr_underrun   ( p0_wr_underrun_i   ),
        .mcb_wr_error      ( p0_wr_error_i      ),
        .mcb_rd_clk        ( p0_rd_clk_i        ),
        .mcb_rd_en         ( p0_rd_en_i         ),
        .mcb_rd_data       ( p0_rd_data_i       ),
        .mcb_rd_full       ( p0_rd_full_i       ),
        .mcb_rd_empty      ( p0_rd_empty_i      ),
        .mcb_rd_count      ( p0_rd_count_i      ),
        .mcb_rd_overflow   ( p0_rd_overflow_i   ),
        .mcb_rd_error      ( p0_rd_error_i      ),
        .mcb_calib_done    ( calib_done_synch   )
        );
    end
  endgenerate

// P1 AXI Bridge Mux
  generate
    if (C_S1_AXI_ENABLE == 0) begin : P1_UI_MCB
      assign  p1_arb_en_i        =  p1_arb_en        ; //
      assign  p1_cmd_clk_i       =  p1_cmd_clk       ; //
      assign  p1_cmd_en_i        =  p1_cmd_en        ; //
      assign  p1_cmd_instr_i     =  p1_cmd_instr     ; // [2:0]
      assign  p1_cmd_bl_i        =  p1_cmd_bl        ; // [5:0]
      assign  p1_cmd_byte_addr_i =  p1_cmd_byte_addr ; // [29:0]
      assign  p1_cmd_empty       =  p1_cmd_empty_i   ; //
      assign  p1_cmd_full        =  p1_cmd_full_i    ; //
      assign  p1_wr_clk_i        =  p1_wr_clk        ; //
      assign  p1_wr_en_i         =  p1_wr_en         ; //
      assign  p1_wr_mask_i       =  p1_wr_mask       ; // [C_P1_MASK_SIZE-1:0]
      assign  p1_wr_data_i       =  p1_wr_data       ; // [C_P1_DATA_PORT_SIZE-1:0]
      assign  p1_wr_full         =  p1_wr_full_i     ; //
      assign  p1_wr_empty        =  p1_wr_empty_i    ; //
      assign  p1_wr_count        =  p1_wr_count_i    ; // [6:0]
      assign  p1_wr_underrun     =  p1_wr_underrun_i ; //
      assign  p1_wr_error        =  p1_wr_error_i    ; //
      assign  p1_rd_clk_i        =  p1_rd_clk        ; //
      assign  p1_rd_en_i         =  p1_rd_en         ; //
      assign  p1_rd_data         =  p1_rd_data_i     ; // [C_P1_DATA_PORT_SIZE-1:0]
      assign  p1_rd_full         =  p1_rd_full_i     ; //
      assign  p1_rd_empty        =  p1_rd_empty_i    ; //
      assign  p1_rd_count        =  p1_rd_count_i    ; // [6:0]
      assign  p1_rd_overflow     =  p1_rd_overflow_i ; //
      assign  p1_rd_error        =  p1_rd_error_i    ; //
    end
    else begin : P1_UI_AXI
      assign  p1_arb_en_i        =  p1_arb_en;
      assign  s1_axi_araddr_i    = s1_axi_araddr & P_S1_AXI_ADDRMASK;
      assign  s1_axi_awaddr_i    = s1_axi_awaddr & P_S1_AXI_ADDRMASK;
      wire                     calib_done_synch;

      mcb_ui_top_synch #(
        .C_SYNCH_WIDTH          ( 1 )
      )
      axi_mcb_synch
      (
        .clk                    ( s1_axi_aclk      ),
        .synch_in               ( uo_done_cal      ),
        .synch_out              ( calib_done_synch )
      );
      axi_mcb #
        (
        .C_FAMILY                ( "spartan6"               ) ,
        .C_S_AXI_ID_WIDTH        ( C_S1_AXI_ID_WIDTH        ) ,
        .C_S_AXI_ADDR_WIDTH      ( C_S1_AXI_ADDR_WIDTH      ) ,
        .C_S_AXI_DATA_WIDTH      ( C_S1_AXI_DATA_WIDTH      ) ,
        .C_S_AXI_SUPPORTS_READ   ( C_S1_AXI_SUPPORTS_READ   ) ,
        .C_S_AXI_SUPPORTS_WRITE  ( C_S1_AXI_SUPPORTS_WRITE  ) ,
        .C_S_AXI_REG_EN0         ( C_S1_AXI_REG_EN0         ) ,
        .C_S_AXI_REG_EN1         ( C_S1_AXI_REG_EN1         ) ,
        .C_S_AXI_SUPPORTS_NARROW_BURST ( C_S1_AXI_SUPPORTS_NARROW_BURST ) ,
        .C_MCB_ADDR_WIDTH        ( 30                       ) ,
        .C_MCB_DATA_WIDTH        ( C_P1_DATA_PORT_SIZE      ) ,
        .C_STRICT_COHERENCY      ( C_S1_AXI_STRICT_COHERENCY    ) ,
        .C_ENABLE_AP             ( C_S1_AXI_ENABLE_AP           )
        )
        p1_axi_mcb
        (
        .aclk              ( s1_axi_aclk        ),
        .aresetn           ( s1_axi_aresetn     ),
        .s_axi_awid        ( s1_axi_awid        ),
        .s_axi_awaddr      ( s1_axi_awaddr_i    ),
        .s_axi_awlen       ( s1_axi_awlen       ),
        .s_axi_awsize      ( s1_axi_awsize      ),
        .s_axi_awburst     ( s1_axi_awburst     ),
        .s_axi_awlock      ( s1_axi_awlock      ),
        .s_axi_awcache     ( s1_axi_awcache     ),
        .s_axi_awprot      ( s1_axi_awprot      ),
        .s_axi_awqos       ( s1_axi_awqos       ),
        .s_axi_awvalid     ( s1_axi_awvalid     ),
        .s_axi_awready     ( s1_axi_awready     ),
        .s_axi_wdata       ( s1_axi_wdata       ),
        .s_axi_wstrb       ( s1_axi_wstrb       ),
        .s_axi_wlast       ( s1_axi_wlast       ),
        .s_axi_wvalid      ( s1_axi_wvalid      ),
        .s_axi_wready      ( s1_axi_wready      ),
        .s_axi_bid         ( s1_axi_bid         ),
        .s_axi_bresp       ( s1_axi_bresp       ),
        .s_axi_bvalid      ( s1_axi_bvalid      ),
        .s_axi_bready      ( s1_axi_bready      ),
        .s_axi_arid        ( s1_axi_arid        ),
        .s_axi_araddr      ( s1_axi_araddr_i    ),
        .s_axi_arlen       ( s1_axi_arlen       ),
        .s_axi_arsize      ( s1_axi_arsize      ),
        .s_axi_arburst     ( s1_axi_arburst     ),
        .s_axi_arlock      ( s1_axi_arlock      ),
        .s_axi_arcache     ( s1_axi_arcache     ),
        .s_axi_arprot      ( s1_axi_arprot      ),
        .s_axi_arqos       ( s1_axi_arqos       ),
        .s_axi_arvalid     ( s1_axi_arvalid     ),
        .s_axi_arready     ( s1_axi_arready     ),
        .s_axi_rid         ( s1_axi_rid         ),
        .s_axi_rdata       ( s1_axi_rdata       ),
        .s_axi_rresp       ( s1_axi_rresp       ),
        .s_axi_rlast       ( s1_axi_rlast       ),
        .s_axi_rvalid      ( s1_axi_rvalid      ),
        .s_axi_rready      ( s1_axi_rready      ),
        .mcb_cmd_clk       ( p1_cmd_clk_i       ),
        .mcb_cmd_en        ( p1_cmd_en_i        ),
        .mcb_cmd_instr     ( p1_cmd_instr_i     ),
        .mcb_cmd_bl        ( p1_cmd_bl_i        ),
        .mcb_cmd_byte_addr ( p1_cmd_byte_addr_i ),
        .mcb_cmd_empty     ( p1_cmd_empty_i     ),
        .mcb_cmd_full      ( p1_cmd_full_i      ),
        .mcb_wr_clk        ( p1_wr_clk_i        ),
        .mcb_wr_en         ( p1_wr_en_i         ),
        .mcb_wr_mask       ( p1_wr_mask_i       ),
        .mcb_wr_data       ( p1_wr_data_i       ),
        .mcb_wr_full       ( p1_wr_full_i       ),
        .mcb_wr_empty      ( p1_wr_empty_i      ),
        .mcb_wr_count      ( p1_wr_count_i      ),
        .mcb_wr_underrun   ( p1_wr_underrun_i   ),
        .mcb_wr_error      ( p1_wr_error_i      ),
        .mcb_rd_clk        ( p1_rd_clk_i        ),
        .mcb_rd_en         ( p1_rd_en_i         ),
        .mcb_rd_data       ( p1_rd_data_i       ),
        .mcb_rd_full       ( p1_rd_full_i       ),
        .mcb_rd_empty      ( p1_rd_empty_i      ),
        .mcb_rd_count      ( p1_rd_count_i      ),
        .mcb_rd_overflow   ( p1_rd_overflow_i   ),
        .mcb_rd_error      ( p1_rd_error_i      ),
        .mcb_calib_done    ( calib_done_synch   )
        );
    end
  endgenerate

// P2 AXI Bridge Mux
  generate
    if (C_S2_AXI_ENABLE == 0) begin : P2_UI_MCB
      assign  p2_arb_en_i        =  p2_arb_en        ; //
      assign  p2_cmd_clk_i       =  p2_cmd_clk       ; //
      assign  p2_cmd_en_i        =  p2_cmd_en        ; //
      assign  p2_cmd_instr_i     =  p2_cmd_instr     ; // [2:0]
      assign  p2_cmd_bl_i        =  p2_cmd_bl        ; // [5:0]
      assign  p2_cmd_byte_addr_i =  p2_cmd_byte_addr ; // [29:0]
      assign  p2_cmd_empty       =  p2_cmd_empty_i   ; //
      assign  p2_cmd_full        =  p2_cmd_full_i    ; //
      assign  p2_wr_clk_i        =  p2_wr_clk        ; //
      assign  p2_wr_en_i         =  p2_wr_en         ; //
      assign  p2_wr_mask_i       =  p2_wr_mask       ; // [3:0]
      assign  p2_wr_data_i       =  p2_wr_data       ; // [31:0]
      assign  p2_wr_full         =  p2_wr_full_i     ; //
      assign  p2_wr_empty        =  p2_wr_empty_i    ; //
      assign  p2_wr_count        =  p2_wr_count_i    ; // [6:0]
      assign  p2_wr_underrun     =  p2_wr_underrun_i ; //
      assign  p2_wr_error        =  p2_wr_error_i    ; //
      assign  p2_rd_clk_i        =  p2_rd_clk        ; //
      assign  p2_rd_en_i         =  p2_rd_en         ; //
      assign  p2_rd_data         =  p2_rd_data_i     ; // [31:0]
      assign  p2_rd_full         =  p2_rd_full_i     ; //
      assign  p2_rd_empty        =  p2_rd_empty_i    ; //
      assign  p2_rd_count        =  p2_rd_count_i    ; // [6:0]
      assign  p2_rd_overflow     =  p2_rd_overflow_i ; //
      assign  p2_rd_error        =  p2_rd_error_i    ; //
    end
    else begin : P2_UI_AXI
      assign  p2_arb_en_i        =  p2_arb_en;
      assign  s2_axi_araddr_i    = s2_axi_araddr & P_S2_AXI_ADDRMASK;
      assign  s2_axi_awaddr_i    = s2_axi_awaddr & P_S2_AXI_ADDRMASK;
      wire                     calib_done_synch;

      mcb_ui_top_synch #(
        .C_SYNCH_WIDTH          ( 1 )
      )
      axi_mcb_synch
      (
        .clk                    ( s2_axi_aclk      ),
        .synch_in               ( uo_done_cal      ),
        .synch_out              ( calib_done_synch )
      );
      axi_mcb #
        (
        .C_FAMILY                ( "spartan6"               ) ,
        .C_S_AXI_ID_WIDTH        ( C_S2_AXI_ID_WIDTH        ) ,
        .C_S_AXI_ADDR_WIDTH      ( C_S2_AXI_ADDR_WIDTH      ) ,
        .C_S_AXI_DATA_WIDTH      ( 32                       ) ,
        .C_S_AXI_SUPPORTS_READ   ( C_S2_AXI_SUPPORTS_READ   ) ,
        .C_S_AXI_SUPPORTS_WRITE  ( C_S2_AXI_SUPPORTS_WRITE  ) ,
        .C_S_AXI_REG_EN0         ( C_S2_AXI_REG_EN0         ) ,
        .C_S_AXI_REG_EN1         ( C_S2_AXI_REG_EN1         ) ,
        .C_S_AXI_SUPPORTS_NARROW_BURST ( C_S2_AXI_SUPPORTS_NARROW_BURST ) ,
        .C_MCB_ADDR_WIDTH        ( 30                       ) ,
        .C_MCB_DATA_WIDTH        ( 32                       ) ,
        .C_STRICT_COHERENCY      ( C_S2_AXI_STRICT_COHERENCY    ) ,
        .C_ENABLE_AP             ( C_S2_AXI_ENABLE_AP           )
        )
        p2_axi_mcb
        (
        .aclk              ( s2_axi_aclk        ),
        .aresetn           ( s2_axi_aresetn     ),
        .s_axi_awid        ( s2_axi_awid        ),
        .s_axi_awaddr      ( s2_axi_awaddr_i    ),
        .s_axi_awlen       ( s2_axi_awlen       ),
        .s_axi_awsize      ( s2_axi_awsize      ),
        .s_axi_awburst     ( s2_axi_awburst     ),
        .s_axi_awlock      ( s2_axi_awlock      ),
        .s_axi_awcache     ( s2_axi_awcache     ),
        .s_axi_awprot      ( s2_axi_awprot      ),
        .s_axi_awqos       ( s2_axi_awqos       ),
        .s_axi_awvalid     ( s2_axi_awvalid     ),
        .s_axi_awready     ( s2_axi_awready     ),
        .s_axi_wdata       ( s2_axi_wdata       ),
        .s_axi_wstrb       ( s2_axi_wstrb       ),
        .s_axi_wlast       ( s2_axi_wlast       ),
        .s_axi_wvalid      ( s2_axi_wvalid      ),
        .s_axi_wready      ( s2_axi_wready      ),
        .s_axi_bid         ( s2_axi_bid         ),
        .s_axi_bresp       ( s2_axi_bresp       ),
        .s_axi_bvalid      ( s2_axi_bvalid      ),
        .s_axi_bready      ( s2_axi_bready      ),
        .s_axi_arid        ( s2_axi_arid        ),
        .s_axi_araddr      ( s2_axi_araddr_i    ),
        .s_axi_arlen       ( s2_axi_arlen       ),
        .s_axi_arsize      ( s2_axi_arsize      ),
        .s_axi_arburst     ( s2_axi_arburst     ),
        .s_axi_arlock      ( s2_axi_arlock      ),
        .s_axi_arcache     ( s2_axi_arcache     ),
        .s_axi_arprot      ( s2_axi_arprot      ),
        .s_axi_arqos       ( s2_axi_arqos       ),
        .s_axi_arvalid     ( s2_axi_arvalid     ),
        .s_axi_arready     ( s2_axi_arready     ),
        .s_axi_rid         ( s2_axi_rid         ),
        .s_axi_rdata       ( s2_axi_rdata       ),
        .s_axi_rresp       ( s2_axi_rresp       ),
        .s_axi_rlast       ( s2_axi_rlast       ),
        .s_axi_rvalid      ( s2_axi_rvalid      ),
        .s_axi_rready      ( s2_axi_rready      ),
        .mcb_cmd_clk       ( p2_cmd_clk_i       ),
        .mcb_cmd_en        ( p2_cmd_en_i        ),
        .mcb_cmd_instr     ( p2_cmd_instr_i     ),
        .mcb_cmd_bl        ( p2_cmd_bl_i        ),
        .mcb_cmd_byte_addr ( p2_cmd_byte_addr_i ),
        .mcb_cmd_empty     ( p2_cmd_empty_i     ),
        .mcb_cmd_full      ( p2_cmd_full_i      ),
        .mcb_wr_clk        ( p2_wr_clk_i        ),
        .mcb_wr_en         ( p2_wr_en_i         ),
        .mcb_wr_mask       ( p2_wr_mask_i       ),
        .mcb_wr_data       ( p2_wr_data_i       ),
        .mcb_wr_full       ( p2_wr_full_i       ),
        .mcb_wr_empty      ( p2_wr_empty_i      ),
        .mcb_wr_count      ( p2_wr_count_i      ),
        .mcb_wr_underrun   ( p2_wr_underrun_i   ),
        .mcb_wr_error      ( p2_wr_error_i      ),
        .mcb_rd_clk        ( p2_rd_clk_i        ),
        .mcb_rd_en         ( p2_rd_en_i         ),
        .mcb_rd_data       ( p2_rd_data_i       ),
        .mcb_rd_full       ( p2_rd_full_i       ),
        .mcb_rd_empty      ( p2_rd_empty_i      ),
        .mcb_rd_count      ( p2_rd_count_i      ),
        .mcb_rd_overflow   ( p2_rd_overflow_i   ),
        .mcb_rd_error      ( p2_rd_error_i      ),
        .mcb_calib_done    ( calib_done_synch   )
        );
    end
  endgenerate

// P3 AXI Bridge Mux
  generate
    if (C_S3_AXI_ENABLE == 0) begin : P3_UI_MCB
      assign  p3_arb_en_i        =  p3_arb_en        ; //
      assign  p3_cmd_clk_i       =  p3_cmd_clk       ; //
      assign  p3_cmd_en_i        =  p3_cmd_en        ; //
      assign  p3_cmd_instr_i     =  p3_cmd_instr     ; // [2:0]
      assign  p3_cmd_bl_i        =  p3_cmd_bl        ; // [5:0]
      assign  p3_cmd_byte_addr_i =  p3_cmd_byte_addr ; // [29:0]
      assign  p3_cmd_empty       =  p3_cmd_empty_i   ; //
      assign  p3_cmd_full        =  p3_cmd_full_i    ; //
      assign  p3_wr_clk_i        =  p3_wr_clk        ; //
      assign  p3_wr_en_i         =  p3_wr_en         ; //
      assign  p3_wr_mask_i       =  p3_wr_mask       ; // [3:0]
      assign  p3_wr_data_i       =  p3_wr_data       ; // [31:0]
      assign  p3_wr_full         =  p3_wr_full_i     ; //
      assign  p3_wr_empty        =  p3_wr_empty_i    ; //
      assign  p3_wr_count        =  p3_wr_count_i    ; // [6:0]
      assign  p3_wr_underrun     =  p3_wr_underrun_i ; //
      assign  p3_wr_error        =  p3_wr_error_i    ; //
      assign  p3_rd_clk_i        =  p3_rd_clk        ; //
      assign  p3_rd_en_i         =  p3_rd_en         ; //
      assign  p3_rd_data         =  p3_rd_data_i     ; // [31:0]
      assign  p3_rd_full         =  p3_rd_full_i     ; //
      assign  p3_rd_empty        =  p3_rd_empty_i    ; //
      assign  p3_rd_count        =  p3_rd_count_i    ; // [6:0]
      assign  p3_rd_overflow     =  p3_rd_overflow_i ; //
      assign  p3_rd_error        =  p3_rd_error_i    ; //
    end
    else begin : P3_UI_AXI
      assign  p3_arb_en_i        =  p3_arb_en;
      assign  s3_axi_araddr_i    = s3_axi_araddr & P_S3_AXI_ADDRMASK;
      assign  s3_axi_awaddr_i    = s3_axi_awaddr & P_S3_AXI_ADDRMASK;
      wire                     calib_done_synch;

      mcb_ui_top_synch #(
        .C_SYNCH_WIDTH          ( 1 )
      )
      axi_mcb_synch
      (
        .clk                    ( s3_axi_aclk      ),
        .synch_in               ( uo_done_cal      ),
        .synch_out              ( calib_done_synch )
      );

      axi_mcb #
        (
        .C_FAMILY                ( "spartan6"               ) ,
        .C_S_AXI_ID_WIDTH        ( C_S3_AXI_ID_WIDTH        ) ,
        .C_S_AXI_ADDR_WIDTH      ( C_S3_AXI_ADDR_WIDTH      ) ,
        .C_S_AXI_DATA_WIDTH      ( 32                       ) ,
        .C_S_AXI_SUPPORTS_READ   ( C_S3_AXI_SUPPORTS_READ   ) ,
        .C_S_AXI_SUPPORTS_WRITE  ( C_S3_AXI_SUPPORTS_WRITE  ) ,
        .C_S_AXI_REG_EN0         ( C_S3_AXI_REG_EN0         ) ,
        .C_S_AXI_REG_EN1         ( C_S3_AXI_REG_EN1         ) ,
        .C_S_AXI_SUPPORTS_NARROW_BURST ( C_S3_AXI_SUPPORTS_NARROW_BURST ) ,
        .C_MCB_ADDR_WIDTH        ( 30                       ) ,
        .C_MCB_DATA_WIDTH        ( 32                       ) ,
        .C_STRICT_COHERENCY      ( C_S3_AXI_STRICT_COHERENCY    ) ,
        .C_ENABLE_AP             ( C_S3_AXI_ENABLE_AP           )
        )
        p3_axi_mcb
        (
        .aclk              ( s3_axi_aclk        ),
        .aresetn           ( s3_axi_aresetn     ),
        .s_axi_awid        ( s3_axi_awid        ),
        .s_axi_awaddr      ( s3_axi_awaddr_i    ),
        .s_axi_awlen       ( s3_axi_awlen       ),
        .s_axi_awsize      ( s3_axi_awsize      ),
        .s_axi_awburst     ( s3_axi_awburst     ),
        .s_axi_awlock      ( s3_axi_awlock      ),
        .s_axi_awcache     ( s3_axi_awcache     ),
        .s_axi_awprot      ( s3_axi_awprot      ),
        .s_axi_awqos       ( s3_axi_awqos       ),
        .s_axi_awvalid     ( s3_axi_awvalid     ),
        .s_axi_awready     ( s3_axi_awready     ),
        .s_axi_wdata       ( s3_axi_wdata       ),
        .s_axi_wstrb       ( s3_axi_wstrb       ),
        .s_axi_wlast       ( s3_axi_wlast       ),
        .s_axi_wvalid      ( s3_axi_wvalid      ),
        .s_axi_wready      ( s3_axi_wready      ),
        .s_axi_bid         ( s3_axi_bid         ),
        .s_axi_bresp       ( s3_axi_bresp       ),
        .s_axi_bvalid      ( s3_axi_bvalid      ),
        .s_axi_bready      ( s3_axi_bready      ),
        .s_axi_arid        ( s3_axi_arid        ),
        .s_axi_araddr      ( s3_axi_araddr_i    ),
        .s_axi_arlen       ( s3_axi_arlen       ),
        .s_axi_arsize      ( s3_axi_arsize      ),
        .s_axi_arburst     ( s3_axi_arburst     ),
        .s_axi_arlock      ( s3_axi_arlock      ),
        .s_axi_arcache     ( s3_axi_arcache     ),
        .s_axi_arprot      ( s3_axi_arprot      ),
        .s_axi_arqos       ( s3_axi_arqos       ),
        .s_axi_arvalid     ( s3_axi_arvalid     ),
        .s_axi_arready     ( s3_axi_arready     ),
        .s_axi_rid         ( s3_axi_rid         ),
        .s_axi_rdata       ( s3_axi_rdata       ),
        .s_axi_rresp       ( s3_axi_rresp       ),
        .s_axi_rlast       ( s3_axi_rlast       ),
        .s_axi_rvalid      ( s3_axi_rvalid      ),
        .s_axi_rready      ( s3_axi_rready      ),
        .mcb_cmd_clk       ( p3_cmd_clk_i       ),
        .mcb_cmd_en        ( p3_cmd_en_i        ),
        .mcb_cmd_instr     ( p3_cmd_instr_i     ),
        .mcb_cmd_bl        ( p3_cmd_bl_i        ),
        .mcb_cmd_byte_addr ( p3_cmd_byte_addr_i ),
        .mcb_cmd_empty     ( p3_cmd_empty_i     ),
        .mcb_cmd_full      ( p3_cmd_full_i      ),
        .mcb_wr_clk        ( p3_wr_clk_i        ),
        .mcb_wr_en         ( p3_wr_en_i         ),
        .mcb_wr_mask       ( p3_wr_mask_i       ),
        .mcb_wr_data       ( p3_wr_data_i       ),
        .mcb_wr_full       ( p3_wr_full_i       ),
        .mcb_wr_empty      ( p3_wr_empty_i      ),
        .mcb_wr_count      ( p3_wr_count_i      ),
        .mcb_wr_underrun   ( p3_wr_underrun_i   ),
        .mcb_wr_error      ( p3_wr_error_i      ),
        .mcb_rd_clk        ( p3_rd_clk_i        ),
        .mcb_rd_en         ( p3_rd_en_i         ),
        .mcb_rd_data       ( p3_rd_data_i       ),
        .mcb_rd_full       ( p3_rd_full_i       ),
        .mcb_rd_empty      ( p3_rd_empty_i      ),
        .mcb_rd_count      ( p3_rd_count_i      ),
        .mcb_rd_overflow   ( p3_rd_overflow_i   ),
        .mcb_rd_error      ( p3_rd_error_i      ),
        .mcb_calib_done    ( calib_done_synch   )
        );
    end
  endgenerate

// P4 AXI Bridge Mux
  generate
    if (C_S4_AXI_ENABLE == 0) begin : P4_UI_MCB
      assign  p4_arb_en_i        =  p4_arb_en        ; //
      assign  p4_cmd_clk_i       =  p4_cmd_clk       ; //
      assign  p4_cmd_en_i        =  p4_cmd_en        ; //
      assign  p4_cmd_instr_i     =  p4_cmd_instr     ; // [2:0]
      assign  p4_cmd_bl_i        =  p4_cmd_bl        ; // [5:0]
      assign  p4_cmd_byte_addr_i =  p4_cmd_byte_addr ; // [29:0]
      assign  p4_cmd_empty       =  p4_cmd_empty_i   ; //
      assign  p4_cmd_full        =  p4_cmd_full_i    ; //
      assign  p4_wr_clk_i        =  p4_wr_clk        ; //
      assign  p4_wr_en_i         =  p4_wr_en         ; //
      assign  p4_wr_mask_i       =  p4_wr_mask       ; // [3:0]
      assign  p4_wr_data_i       =  p4_wr_data       ; // [31:0]
      assign  p4_wr_full         =  p4_wr_full_i     ; //
      assign  p4_wr_empty        =  p4_wr_empty_i    ; //
      assign  p4_wr_count        =  p4_wr_count_i    ; // [6:0]
      assign  p4_wr_underrun     =  p4_wr_underrun_i ; //
      assign  p4_wr_error        =  p4_wr_error_i    ; //
      assign  p4_rd_clk_i        =  p4_rd_clk        ; //
      assign  p4_rd_en_i         =  p4_rd_en         ; //
      assign  p4_rd_data         =  p4_rd_data_i     ; // [31:0]
      assign  p4_rd_full         =  p4_rd_full_i     ; //
      assign  p4_rd_empty        =  p4_rd_empty_i    ; //
      assign  p4_rd_count        =  p4_rd_count_i    ; // [6:0]
      assign  p4_rd_overflow     =  p4_rd_overflow_i ; //
      assign  p4_rd_error        =  p4_rd_error_i    ; //
    end
    else begin : P4_UI_AXI
      assign  p4_arb_en_i        =  p4_arb_en;
      assign  s4_axi_araddr_i    = s4_axi_araddr & P_S4_AXI_ADDRMASK;
      assign  s4_axi_awaddr_i    = s4_axi_awaddr & P_S4_AXI_ADDRMASK;
      wire                     calib_done_synch;

      mcb_ui_top_synch #(
        .C_SYNCH_WIDTH          ( 1 )
      )
      axi_mcb_synch
      (
        .clk                    ( s4_axi_aclk      ),
        .synch_in               ( uo_done_cal      ),
        .synch_out              ( calib_done_synch )
      );

      axi_mcb #
        (
        .C_FAMILY                ( "spartan6"               ) ,
        .C_S_AXI_ID_WIDTH        ( C_S4_AXI_ID_WIDTH        ) ,
        .C_S_AXI_ADDR_WIDTH      ( C_S4_AXI_ADDR_WIDTH      ) ,
        .C_S_AXI_DATA_WIDTH      ( 32                       ) ,
        .C_S_AXI_SUPPORTS_READ   ( C_S4_AXI_SUPPORTS_READ   ) ,
        .C_S_AXI_SUPPORTS_WRITE  ( C_S4_AXI_SUPPORTS_WRITE  ) ,
        .C_S_AXI_REG_EN0         ( C_S4_AXI_REG_EN0         ) ,
        .C_S_AXI_REG_EN1         ( C_S4_AXI_REG_EN1         ) ,
        .C_S_AXI_SUPPORTS_NARROW_BURST ( C_S4_AXI_SUPPORTS_NARROW_BURST ) ,
        .C_MCB_ADDR_WIDTH        ( 30                       ) ,
        .C_MCB_DATA_WIDTH        ( 32                       ) ,
        .C_STRICT_COHERENCY      ( C_S4_AXI_STRICT_COHERENCY    ) ,
        .C_ENABLE_AP             ( C_S4_AXI_ENABLE_AP           )
        )
        p4_axi_mcb
        (
        .aclk              ( s4_axi_aclk        ),
        .aresetn           ( s4_axi_aresetn     ),
        .s_axi_awid        ( s4_axi_awid        ),
        .s_axi_awaddr      ( s4_axi_awaddr_i    ),
        .s_axi_awlen       ( s4_axi_awlen       ),
        .s_axi_awsize      ( s4_axi_awsize      ),
        .s_axi_awburst     ( s4_axi_awburst     ),
        .s_axi_awlock      ( s4_axi_awlock      ),
        .s_axi_awcache     ( s4_axi_awcache     ),
        .s_axi_awprot      ( s4_axi_awprot      ),
        .s_axi_awqos       ( s4_axi_awqos       ),
        .s_axi_awvalid     ( s4_axi_awvalid     ),
        .s_axi_awready     ( s4_axi_awready     ),
        .s_axi_wdata       ( s4_axi_wdata       ),
        .s_axi_wstrb       ( s4_axi_wstrb       ),
        .s_axi_wlast       ( s4_axi_wlast       ),
        .s_axi_wvalid      ( s4_axi_wvalid      ),
        .s_axi_wready      ( s4_axi_wready      ),
        .s_axi_bid         ( s4_axi_bid         ),
        .s_axi_bresp       ( s4_axi_bresp       ),
        .s_axi_bvalid      ( s4_axi_bvalid      ),
        .s_axi_bready      ( s4_axi_bready      ),
        .s_axi_arid        ( s4_axi_arid        ),
        .s_axi_araddr      ( s4_axi_araddr_i    ),
        .s_axi_arlen       ( s4_axi_arlen       ),
        .s_axi_arsize      ( s4_axi_arsize      ),
        .s_axi_arburst     ( s4_axi_arburst     ),
        .s_axi_arlock      ( s4_axi_arlock      ),
        .s_axi_arcache     ( s4_axi_arcache     ),
        .s_axi_arprot      ( s4_axi_arprot      ),
        .s_axi_arqos       ( s4_axi_arqos       ),
        .s_axi_arvalid     ( s4_axi_arvalid     ),
        .s_axi_arready     ( s4_axi_arready     ),
        .s_axi_rid         ( s4_axi_rid         ),
        .s_axi_rdata       ( s4_axi_rdata       ),
        .s_axi_rresp       ( s4_axi_rresp       ),
        .s_axi_rlast       ( s4_axi_rlast       ),
        .s_axi_rvalid      ( s4_axi_rvalid      ),
        .s_axi_rready      ( s4_axi_rready      ),
        .mcb_cmd_clk       ( p4_cmd_clk_i       ),
        .mcb_cmd_en        ( p4_cmd_en_i        ),
        .mcb_cmd_instr     ( p4_cmd_instr_i     ),
        .mcb_cmd_bl        ( p4_cmd_bl_i        ),
        .mcb_cmd_byte_addr ( p4_cmd_byte_addr_i ),
        .mcb_cmd_empty     ( p4_cmd_empty_i     ),
        .mcb_cmd_full      ( p4_cmd_full_i      ),
        .mcb_wr_clk        ( p4_wr_clk_i        ),
        .mcb_wr_en         ( p4_wr_en_i         ),
        .mcb_wr_mask       ( p4_wr_mask_i       ),
        .mcb_wr_data       ( p4_wr_data_i       ),
        .mcb_wr_full       ( p4_wr_full_i       ),
        .mcb_wr_empty      ( p4_wr_empty_i      ),
        .mcb_wr_count      ( p4_wr_count_i      ),
        .mcb_wr_underrun   ( p4_wr_underrun_i   ),
        .mcb_wr_error      ( p4_wr_error_i      ),
        .mcb_rd_clk        ( p4_rd_clk_i        ),
        .mcb_rd_en         ( p4_rd_en_i         ),
        .mcb_rd_data       ( p4_rd_data_i       ),
        .mcb_rd_full       ( p4_rd_full_i       ),
        .mcb_rd_empty      ( p4_rd_empty_i      ),
        .mcb_rd_count      ( p4_rd_count_i      ),
        .mcb_rd_overflow   ( p4_rd_overflow_i   ),
        .mcb_rd_error      ( p4_rd_error_i      ),
        .mcb_calib_done    ( calib_done_synch   )
        );
    end
  endgenerate

// P5 AXI Bridge Mux
  generate
    if (C_S5_AXI_ENABLE == 0) begin : P5_UI_MCB
      assign  p5_arb_en_i        =  p5_arb_en        ; //
      assign  p5_cmd_clk_i       =  p5_cmd_clk       ; //
      assign  p5_cmd_en_i        =  p5_cmd_en        ; //
      assign  p5_cmd_instr_i     =  p5_cmd_instr     ; // [2:0]
      assign  p5_cmd_bl_i        =  p5_cmd_bl        ; // [5:0]
      assign  p5_cmd_byte_addr_i =  p5_cmd_byte_addr ; // [29:0]
      assign  p5_cmd_empty       =  p5_cmd_empty_i   ; //
      assign  p5_cmd_full        =  p5_cmd_full_i    ; //
      assign  p5_wr_clk_i        =  p5_wr_clk        ; //
      assign  p5_wr_en_i         =  p5_wr_en         ; //
      assign  p5_wr_mask_i       =  p5_wr_mask       ; // [3:0]
      assign  p5_wr_data_i       =  p5_wr_data       ; // [31:0]
      assign  p5_wr_full         =  p5_wr_full_i     ; //
      assign  p5_wr_empty        =  p5_wr_empty_i    ; //
      assign  p5_wr_count        =  p5_wr_count_i    ; // [6:0]
      assign  p5_wr_underrun     =  p5_wr_underrun_i ; //
      assign  p5_wr_error        =  p5_wr_error_i    ; //
      assign  p5_rd_clk_i        =  p5_rd_clk        ; //
      assign  p5_rd_en_i         =  p5_rd_en         ; //
      assign  p5_rd_data         =  p5_rd_data_i     ; // [31:0]
      assign  p5_rd_full         =  p5_rd_full_i     ; //
      assign  p5_rd_empty        =  p5_rd_empty_i    ; //
      assign  p5_rd_count        =  p5_rd_count_i    ; // [6:0]
      assign  p5_rd_overflow     =  p5_rd_overflow_i ; //
      assign  p5_rd_error        =  p5_rd_error_i    ; //
    end
    else begin : P5_UI_AXI
      assign  p5_arb_en_i        =  p5_arb_en;
      assign  s5_axi_araddr_i    = s5_axi_araddr & P_S5_AXI_ADDRMASK;
      assign  s5_axi_awaddr_i    = s5_axi_awaddr & P_S5_AXI_ADDRMASK;
      wire                     calib_done_synch;

      mcb_ui_top_synch #(
        .C_SYNCH_WIDTH          ( 1 )
      )
      axi_mcb_synch
      (
        .clk                    ( s5_axi_aclk      ),
        .synch_in               ( uo_done_cal      ),
        .synch_out              ( calib_done_synch )
      );

      axi_mcb #
        (
        .C_FAMILY                ( "spartan6"               ) ,
        .C_S_AXI_ID_WIDTH        ( C_S5_AXI_ID_WIDTH        ) ,
        .C_S_AXI_ADDR_WIDTH      ( C_S5_AXI_ADDR_WIDTH      ) ,
        .C_S_AXI_DATA_WIDTH      ( 32                       ) ,
        .C_S_AXI_SUPPORTS_READ   ( C_S5_AXI_SUPPORTS_READ   ) ,
        .C_S_AXI_SUPPORTS_WRITE  ( C_S5_AXI_SUPPORTS_WRITE  ) ,
        .C_S_AXI_REG_EN0         ( C_S5_AXI_REG_EN0         ) ,
        .C_S_AXI_REG_EN1         ( C_S5_AXI_REG_EN1         ) ,
        .C_S_AXI_SUPPORTS_NARROW_BURST ( C_S5_AXI_SUPPORTS_NARROW_BURST ) ,
        .C_MCB_ADDR_WIDTH        ( 30                       ) ,
        .C_MCB_DATA_WIDTH        ( 32                       ) ,
        .C_STRICT_COHERENCY      ( C_S5_AXI_STRICT_COHERENCY    ) ,
        .C_ENABLE_AP             ( C_S5_AXI_ENABLE_AP           )
        )
        p5_axi_mcb
        (
        .aclk              ( s5_axi_aclk        ),
        .aresetn           ( s5_axi_aresetn     ),
        .s_axi_awid        ( s5_axi_awid        ),
        .s_axi_awaddr      ( s5_axi_awaddr_i    ),
        .s_axi_awlen       ( s5_axi_awlen       ),
        .s_axi_awsize      ( s5_axi_awsize      ),
        .s_axi_awburst     ( s5_axi_awburst     ),
        .s_axi_awlock      ( s5_axi_awlock      ),
        .s_axi_awcache     ( s5_axi_awcache     ),
        .s_axi_awprot      ( s5_axi_awprot      ),
        .s_axi_awqos       ( s5_axi_awqos       ),
        .s_axi_awvalid     ( s5_axi_awvalid     ),
        .s_axi_awready     ( s5_axi_awready     ),
        .s_axi_wdata       ( s5_axi_wdata       ),
        .s_axi_wstrb       ( s5_axi_wstrb       ),
        .s_axi_wlast       ( s5_axi_wlast       ),
        .s_axi_wvalid      ( s5_axi_wvalid      ),
        .s_axi_wready      ( s5_axi_wready      ),
        .s_axi_bid         ( s5_axi_bid         ),
        .s_axi_bresp       ( s5_axi_bresp       ),
        .s_axi_bvalid      ( s5_axi_bvalid      ),
        .s_axi_bready      ( s5_axi_bready      ),
        .s_axi_arid        ( s5_axi_arid        ),
        .s_axi_araddr      ( s5_axi_araddr_i    ),
        .s_axi_arlen       ( s5_axi_arlen       ),
        .s_axi_arsize      ( s5_axi_arsize      ),
        .s_axi_arburst     ( s5_axi_arburst     ),
        .s_axi_arlock      ( s5_axi_arlock      ),
        .s_axi_arcache     ( s5_axi_arcache     ),
        .s_axi_arprot      ( s5_axi_arprot      ),
        .s_axi_arqos       ( s5_axi_arqos       ),
        .s_axi_arvalid     ( s5_axi_arvalid     ),
        .s_axi_arready     ( s5_axi_arready     ),
        .s_axi_rid         ( s5_axi_rid         ),
        .s_axi_rdata       ( s5_axi_rdata       ),
        .s_axi_rresp       ( s5_axi_rresp       ),
        .s_axi_rlast       ( s5_axi_rlast       ),
        .s_axi_rvalid      ( s5_axi_rvalid      ),
        .s_axi_rready      ( s5_axi_rready      ),
        .mcb_cmd_clk       ( p5_cmd_clk_i       ),
        .mcb_cmd_en        ( p5_cmd_en_i        ),
        .mcb_cmd_instr     ( p5_cmd_instr_i     ),
        .mcb_cmd_bl        ( p5_cmd_bl_i        ),
        .mcb_cmd_byte_addr ( p5_cmd_byte_addr_i ),
        .mcb_cmd_empty     ( p5_cmd_empty_i     ),
        .mcb_cmd_full      ( p5_cmd_full_i      ),
        .mcb_wr_clk        ( p5_wr_clk_i        ),
        .mcb_wr_en         ( p5_wr_en_i         ),
        .mcb_wr_mask       ( p5_wr_mask_i       ),
        .mcb_wr_data       ( p5_wr_data_i       ),
        .mcb_wr_full       ( p5_wr_full_i       ),
        .mcb_wr_empty      ( p5_wr_empty_i      ),
        .mcb_wr_count      ( p5_wr_count_i      ),
        .mcb_wr_underrun   ( p5_wr_underrun_i   ),
        .mcb_wr_error      ( p5_wr_error_i      ),
        .mcb_rd_clk        ( p5_rd_clk_i        ),
        .mcb_rd_en         ( p5_rd_en_i         ),
        .mcb_rd_data       ( p5_rd_data_i       ),
        .mcb_rd_full       ( p5_rd_full_i       ),
        .mcb_rd_empty      ( p5_rd_empty_i      ),
        .mcb_rd_count      ( p5_rd_count_i      ),
        .mcb_rd_overflow   ( p5_rd_overflow_i   ),
        .mcb_rd_error      ( p5_rd_error_i      ),
        .mcb_calib_done    ( calib_done_synch   )
        );
    end
  endgenerate

endmodule

