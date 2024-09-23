import pandas as pd
import matplotlib.pyplot as plt
import numpy as np

def read_main_csv(file_path):
    df = pd.read_csv(file_path, skiprows=1)
    time = df['Time']
    hotter = np.add(df['Hotter'], 273.15)
    colder = np.add(df['Colder'], 273.15)

    return time, hotter, colder

def read_secondary_csv(file_path):
    df = pd.read_csv(file_path, header=None)
    return df[0]

def plot_graphs(main_csv, cooler_csv, hotter_csv, name):

    time, fds_hotter, fds_colder = read_main_csv(main_csv)

    # Read secondary data
    firevox_time = [0.1 * x for x in range(0, 24001)]

    firevox_hotter = read_secondary_csv(hotter_csv)
    firevox_colder = read_secondary_csv(cooler_csv)

    # Plot Time vs. Inner Temp
    plt.figure(figsize=(8, 6))
    plt.plot(time, fds_hotter, label='FDS', color='green')
    plt.plot(firevox_time, firevox_hotter, label='FireVox', linestyle='--', color='red')
    plt.xlabel('Time [s]')
    plt.ylabel('Temperature [K]')
    plt.title(f'Slab temperature of middle point of hotter plane for {name} configuration')
    plt.legend()
    plt.grid(True)
    # plt.show()
    plt.savefig(f"rad_{name}_hotter.png")

    # Plot Time vs. Gas Temp
    plt.figure(figsize=(8, 6))
    plt.plot(time, fds_colder, label='FDS', color='green')
    plt.plot(firevox_time, firevox_colder, label='FireVox', linestyle='--', color='red')
    plt.xlabel('Time [s]')
    plt.ylabel('Temperature [K]')
    plt.title(f'Slab temperature of middle point of colder plane for {name} configuration')
    plt.legend()
    plt.grid(True)
    # plt.show()
    plt.savefig(f"rad_{name}_colder.png")


# Example usage
fds_para_csv = 'radiation/fds/geom_para_devc.csv'
firevox_cooler_para_csv = 'radiation/firevox/cooler_parallel.csv'
firevox_hotter_para_csv = 'radiation/firevox/hotter_parallel.csv'

fds_perp_csv = 'radiation/fds/geom_perp_devc.csv'
firevox_cooler_perp_csv = 'radiation/firevox/cooler_perpendicular.csv'
firevox_hotter_perp_csv = 'radiation/firevox/hotter_perpendicular.csv'

plot_graphs(fds_para_csv, firevox_cooler_para_csv, firevox_hotter_para_csv, "parallel")
plot_graphs(fds_perp_csv, firevox_cooler_perp_csv, firevox_hotter_perp_csv, "perpendicular")
