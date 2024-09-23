import pandas as pd
import matplotlib.pyplot as plt
import numpy as np

def read_main_csv(file_path):
    df = pd.read_csv(file_path, skiprows=1)
    time = df['Time']
    inner_temp = np.add(df['inner temp'], 273.15)
    gas_temp = np.add(df['gas temp'], 273.15)
    surface_temp = np.add(df['surface temp'], 273.15)

    return time, inner_temp, gas_temp, surface_temp

def read_secondary_csv(file_path):
    df = pd.read_csv(file_path, header=None)
    return df[0]


# Main function to plot the graphs
def plot_graphs(main_csv, secondary_inner_temp_csv, secondary_gas_temp_csv, firevox_surface_temp_csv):
    # Read data from the main CSV
    time, inner_temp, gas_temp, surface_temp = read_main_csv(main_csv)

    # Read secondary data
    firevox_time = [0.5 * x for x in range(0, 3601)]
    firevox_inner_temp = read_secondary_csv(secondary_inner_temp_csv)
    firevox_gas_temp = read_secondary_csv(secondary_gas_temp_csv)
    firevox_surface_temp = read_secondary_csv(firevox_surface_temp_csv)

    # Plot Time vs. Inner Temp
    plt.figure(figsize=(8, 6))
    plt.plot(time, inner_temp, label='FDS', color='green')
    plt.plot(firevox_time, firevox_inner_temp, label='FireVox', linestyle='--', color='red')
    plt.xlabel('Time [s]')
    plt.ylabel('Temperature [K]')
    plt.title('Inner temperature of slab at (15, 15, 0) cm')
    plt.legend()
    plt.ylim(1260, 1300)
    plt.grid(True)
    plt.savefig("conv_inner.png")
    plt.show()

    # Plot Time vs. Gas Temp
    plt.figure(figsize=(8, 6))
    plt.plot(time, gas_temp, label='FDS', color='green')
    plt.plot(firevox_time, firevox_gas_temp, label='FireVox', linestyle='--', color='red')
    plt.xlabel('Time [s]')
    plt.ylabel('Temperature [K]')
    plt.title('Air temperature at (15, 15, 120) cm')
    plt.legend()
    plt.grid(True)
    plt.savefig("conv_air.png")
    plt.show()

    # Plot Time vs. Surface Temp
    plt.figure(figsize=(8, 6))
    plt.plot(time, surface_temp, label='FDS', color='green')
    plt.plot(firevox_time, firevox_surface_temp, label='FireVox', linestyle='--', color='red')
    plt.xlabel('Time [s]')
    plt.ylabel('Temperature [K]')
    plt.title('Slab surface temperature at (15, 15, 100) cm')
    plt.legend()
    plt.grid(True)
    plt.savefig("conv_surf.png")
    plt.show()


# Example usage
main_csv = 'convection/convective_cooling_devc.csv'
firevox_inner_temp_csv = 'convection/hotPlateThermometer_3600.csv'
firevox_gas_temp_csv = 'convection/gasThermometer_3600.csv'
firevox_surface_temp_csv = 'convection/surfaceThermometer_3600.csv'

plot_graphs(main_csv, firevox_inner_temp_csv, firevox_gas_temp_csv, firevox_surface_temp_csv)
